package com.koolearn.bms.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.koolearn.bms.annotation.RequireRole;
import com.koolearn.bms.entity.SysConfig;
import com.koolearn.bms.mapper.SysConfigMapper;
import com.koolearn.bms.service.SysOperationLogService;
import com.koolearn.bms.util.Result;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据备份（需求 2.6.3）：手动全量备份、备份策略配置（每周日凌晨2点全量、每日增量、保留周期）。
 * 定时任务见 BackupScheduler。备份通过 mysqldump 生成 SQL 文件。
 */
@RequireRole("admin")
@RestController
@RequestMapping("/backup")
public class BackupController {

    @Value("${spring.datasource.url}")
    private String datasourceUrl;

    @Value("${spring.datasource.username}")
    private String dbUser;

    @Value("${spring.datasource.password}")
    private String dbPassword;

    private final SysConfigMapper sysConfigMapper;
    private final SysOperationLogService sysLogService;

    public BackupController(SysConfigMapper sysConfigMapper, SysOperationLogService sysLogService) {
        this.sysConfigMapper = sysConfigMapper;
        this.sysLogService = sysLogService;
    }

    static String extractDbName(String url) {
        if (url == null) return "bms_db";
        int idx = url.lastIndexOf("/");
        if (idx < 0) return "bms_db";
        String tail = url.substring(idx + 1);
        int q = tail.indexOf("?");
        return q > 0 ? tail.substring(0, q) : tail;
    }

    public static String getConfigValue(SysConfigMapper mapper, String key, String def) {
        SysConfig cfg = mapper.selectOne(new LambdaQueryWrapper<SysConfig>().eq(SysConfig::getConfigKey, key).last("limit 1"));
        return cfg == null || cfg.getConfigValue() == null || cfg.getConfigValue().isEmpty() ? def : cfg.getConfigValue();
    }

    @PostMapping("/db")
    public Result<String> backup(@RequestAttribute("username") String operator, HttpServletRequest request) {
        try {
            String message = doFullBackup();
            sysLogService.log(operator, "数据备份", "手动全量备份: " + message, getIp(request));
            return Result.success(message);
        } catch (Exception e) {
            sysLogService.log(operator, "数据备份", "手动全量备份失败: " + e.getMessage(), getIp(request));
            return Result.fail("备份异常: " + e.getMessage());
        }
    }

    /** 执行全量备份，返回备份文件路径。 */
    public String doFullBackup() throws Exception {
        String dbName = extractDbName(datasourceUrl);
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String dir = backupDir();
        new java.io.File(dir).mkdirs();
        String file = dir + "/" + dbName + "_full_" + time + ".sql";
        List<String> cmd = new ArrayList<>();
        cmd.add("mysqldump");
        cmd.add("-u" + dbUser);
        cmd.add("--databases");
        cmd.add(dbName);
        cmd.add("--result-file=" + file);
        ProcessBuilder pb = new ProcessBuilder(cmd).redirectErrorStream(true);
        // 通过环境变量传密码，避免密码出现在进程列表
        pb.environment().put("MYSQL_PWD", dbPassword == null ? "" : dbPassword);
        Process p = pb.start();
        if (!p.waitFor(10, java.util.concurrent.TimeUnit.MINUTES)) {
            p.destroyForcibly();
            throw new RuntimeException("备份超时");
        }
        if (p.exitValue() != 0) {
            throw new RuntimeException("备份失败: " + readOutput(p));
        }
        cleanExpired();
        return "备份成功: " + file;
    }

    /** 增量备份：导出当日有变动的流水/日志表。 */
    public String doIncrementalBackup() throws Exception {
        String dbName = extractDbName(datasourceUrl);
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String dir = backupDir();
        new java.io.File(dir).mkdirs();
        String file = dir + "/" + dbName + "_incr_" + time + ".sql";
        String today = LocalDateTime.now().toLocalDate().toString();
        List<String> cmd = new ArrayList<>();
        cmd.add("mysqldump");
        cmd.add("-u" + dbUser);
        cmd.add(dbName);
        cmd.add("in_record");
        cmd.add("tb_out_record");
        cmd.add("sys_stock_alert");
        cmd.add("sys_login_log");
        cmd.add("sys_operation_log");
        cmd.add("--where=create_time >= '" + today + "'");
        cmd.add("--result-file=" + file);
        ProcessBuilder pb = new ProcessBuilder(cmd).redirectErrorStream(true);
        pb.environment().put("MYSQL_PWD", dbPassword == null ? "" : dbPassword);
        Process p = pb.start();
        if (!p.waitFor(10, java.util.concurrent.TimeUnit.MINUTES)) {
            p.destroyForcibly();
            throw new RuntimeException("增量备份超时");
        }
        if (p.exitValue() != 0) {
            throw new RuntimeException("增量备份失败: " + readOutput(p));
        }
        cleanExpired();
        return "增量备份成功: " + file;
    }

    /** 读取备份配置（cron、保留天数、目录）。 */
    @GetMapping("/config")
    public Result<Map<String, String>> config() {
        Map<String, String> data = new HashMap<>();
        data.put("fullCron", getConfigValue(sysConfigMapper, "backup.full.cron", "0 0 2 * * 0"));
        data.put("incrCron", getConfigValue(sysConfigMapper, "backup.incr.cron", "0 0 2 * * ?"));
        data.put("retention", getConfigValue(sysConfigMapper, "backup.retention", "30"));
        data.put("dir", getConfigValue(sysConfigMapper, "backup.dir", "bms_backup"));
        return Result.success(data);
    }

    /** 保存备份策略配置。 */
    @PostMapping("/config")
    public Result<String> saveConfig(@RequestBody Map<String, String> body,
                                     @RequestAttribute("username") String operator,
                                     HttpServletRequest request) {
        saveConfigValue("backup.full.cron", body.get("fullCron"));
        saveConfigValue("backup.incr.cron", body.get("incrCron"));
        saveConfigValue("backup.retention", body.get("retention"));
        saveConfigValue("backup.dir", body.get("dir"));
        sysLogService.log(operator, "数据备份", "更新自动备份策略配置", getIp(request));
        return Result.success("备份策略已保存");
    }

    private void saveConfigValue(String key, String value) {
        if (value == null || value.isEmpty()) return;
        SysConfig cfg = sysConfigMapper.selectOne(new LambdaQueryWrapper<SysConfig>().eq(SysConfig::getConfigKey, key).last("limit 1"));
        if (cfg == null) {
            cfg = new SysConfig();
            cfg.setConfigKey(key);
            cfg.setConfigValue(value);
            sysConfigMapper.insert(cfg);
        } else {
            cfg.setConfigValue(value);
            sysConfigMapper.updateById(cfg);
        }
    }

    private String backupDir() {
        String dir = getConfigValue(sysConfigMapper, "backup.dir", "bms_backup");
        return System.getProperty("user.home") + "/" + dir;
    }

    /** 按保留周期清理过期备份文件。 */
    private void cleanExpired() {
        try {
            int retention = Integer.parseInt(getConfigValue(sysConfigMapper, "backup.retention", "30"));
            java.io.File dir = new java.io.File(backupDir());
            java.io.File[] files = dir.listFiles((d, name) -> name.endsWith(".sql"));
            if (files == null) return;
            long cutoff = System.currentTimeMillis() - retention * 24L * 3600 * 1000;
            for (java.io.File f : files) {
                if (f.lastModified() < cutoff) {
                    f.delete();
                }
            }
        } catch (Exception ignore) {
            // 清理失败不影响备份
        }
    }

    private String readOutput(Process p) throws Exception {
        StringBuilder output = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            String line;
            while ((line = br.readLine()) != null) output.append(line);
        }
        return output.toString();
    }

    private String getIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) ip = request.getHeader("X-Real-IP");
        if (ip == null || ip.isEmpty()) ip = request.getRemoteAddr();
        return ip;
    }
}
