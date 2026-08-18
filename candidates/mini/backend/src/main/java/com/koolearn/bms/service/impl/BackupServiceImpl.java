package com.koolearn.bms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.koolearn.bms.entity.SysBackupRecord;
import com.koolearn.bms.mapper.SysBackupRecordMapper;
import com.koolearn.bms.service.BackupService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
public class BackupServiceImpl extends ServiceImpl<SysBackupRecordMapper, SysBackupRecord> implements BackupService {

    @Value("${spring.datasource.url}")
    private String datasourceUrl;

    @Value("${spring.datasource.username}")
    private String dbUser;

    @Value("${spring.datasource.password}")
    private String dbPassword;

    @Value("${backup.directory:${user.home}/bms_backup}")
    private String backupDir;

    private static String extractDbName(String url) {
        if (url == null) return "bms";
        int idx = url.lastIndexOf("/");
        if (idx < 0) return "bms";
        String tail = url.substring(idx + 1);
        int q = tail.indexOf("?");
        return q > 0 ? tail.substring(0, q) : tail;
    }

    @Override
    public String backup(String type) {
        try {
            String dbName = extractDbName(datasourceUrl);
            String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            File dir = new File(backupDir);
            dir.mkdirs();
            String file = new File(dir, dbName + "_" + type + "_" + time + ".sql").getAbsolutePath();

            List<String> cmd = new ArrayList<>();
            cmd.add("mysqldump");
            cmd.add("-u" + dbUser);
            cmd.add("--databases");
            cmd.add(dbName);
            cmd.add("--result-file=" + file);
            if ("incremental".equals(type)) {
                // MySQL 增量备份需要 binlog；这里退化为基于数据表的逻辑备份（带 --single-transaction 的完整备份）
                cmd.add("--single-transaction");
            }

            ProcessBuilder pb = new ProcessBuilder(cmd);
            // 避免密码出现在进程列表中，使用 MYSQL_PWD 环境变量
            pb.environment().put("MYSQL_PWD", dbPassword == null ? "" : dbPassword);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            p.waitFor();

            if (p.exitValue() != 0) {
                String out = new String(p.getInputStream().readAllBytes());
                log.error("数据库备份失败: {}", out);
                return "备份失败: " + out;
            }

            File f = new File(file);
            SysBackupRecord record = new SysBackupRecord();
            record.setBackupType(type);
            record.setFilePath(file);
            record.setFileSize(f.length());
            record.setStatus("SUCCESS");
            record.setCreateTime(LocalDateTime.now());
            save(record);
            log.info("数据库备份成功: {}", file);
            return "备份成功: " + file;
        } catch (Exception e) {
            log.error("数据库备份异常", e);
            return "备份异常: " + e.getMessage();
        }
    }

    @Override
    public IPage<SysBackupRecord> listRecords(Long pageNum, Long pageSize) {
        Page<SysBackupRecord> page = new Page<>(pageNum, pageSize);
        return page(page, new LambdaQueryWrapper<SysBackupRecord>().orderByDesc(SysBackupRecord::getCreateTime));
    }

    @Override
    public void cleanupExpired(int retentionDays) {
        if (retentionDays <= 0) return;
        File dir = new File(backupDir);
        if (!dir.isDirectory()) return;
        File[] files = dir.listFiles((d, name) -> name.endsWith(".sql"));
        if (files == null) return;
        long cutoff = System.currentTimeMillis() - retentionDays * 24L * 3600 * 1000;
        Arrays.stream(files)
                .filter(f -> f.lastModified() < cutoff)
                .forEach(f -> {
                    if (f.delete()) log.info("已清理过期备份: {}", f.getName());
                });
        // 清理数据库记录
        List<SysBackupRecord> expired = list(new LambdaQueryWrapper<SysBackupRecord>()
                .lt(SysBackupRecord::getCreateTime, LocalDateTime.now().minusDays(retentionDays)));
        if (!expired.isEmpty()) {
            removeByIds(expired.stream().map(SysBackupRecord::getId).collect(java.util.stream.Collectors.toList()));
        }
    }
}
