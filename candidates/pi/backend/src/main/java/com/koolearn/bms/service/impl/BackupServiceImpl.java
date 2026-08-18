package com.koolearn.bms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.koolearn.bms.entity.BackupRecord;
import com.koolearn.bms.mapper.BackupRecordMapper;
import com.koolearn.bms.service.BackupService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class BackupServiceImpl implements BackupService {

    private final BackupRecordMapper backupRecordMapper;

    @Value("${spring.datasource.url}")
    private String datasourceUrl;

    @Value("${spring.datasource.username}")
    private String dbUser;

    @Value("${spring.datasource.password}")
    private String dbPassword;

    public BackupServiceImpl(BackupRecordMapper backupRecordMapper) {
        this.backupRecordMapper = backupRecordMapper;
    }

    @Override
    public String backup(String type) {
        BackupRecord record = new BackupRecord();
        record.setBackupType(type);
        record.setCreateTime(LocalDateTime.now());
        try {
            String dbName = extractDbName(datasourceUrl);
            String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String dir = System.getProperty("user.home") + "/bms_backup";
            new File(dir).mkdirs();
            String file = dir + "/" + dbName + "_" + type + "_" + time + ".sql";

            List<String> cmd = new ArrayList<>();
            cmd.add("mysqldump");
            cmd.add("-u" + dbUser);
            cmd.add("--single-transaction");
            cmd.add("--databases");
            cmd.add(dbName);
            cmd.add("--result-file=" + file);

            ProcessBuilder pb = new ProcessBuilder(cmd);
            // 密码通过 MYSQL_PWD 环境变量传递，避免出现在命令行（ps 可见）
            if (dbPassword != null && !dbPassword.isEmpty()) {
                pb.environment().put("MYSQL_PWD", dbPassword);
            }
            pb.redirectErrorStream(true);
            Process p = pb.start();
            p.waitFor();

            StringBuilder output = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null) output.append(line);
            }
            if (p.exitValue() == 0) {
                File f = new File(file);
                record.setFilePath(file);
                record.setFileSize(f.exists() ? f.length() : 0L);
                record.setStatus("SUCCESS");
                record.setMessage("备份成功");
                backupRecordMapper.insert(record);
                log.info("数据库备份成功: {}", file);
                return "备份成功: " + file;
            }
            record.setStatus("FAILED");
            record.setMessage("mysqldump失败: " + truncate(output.toString()));
            backupRecordMapper.insert(record);
            return "备份失败: " + truncate(output.toString());
        } catch (Exception e) {
            record.setStatus("FAILED");
            record.setMessage("备份异常(mysqldump不可用?): " + e.getMessage());
            backupRecordMapper.insert(record);
            log.warn("数据库备份异常: {}", e.getMessage());
            return "备份异常: " + e.getMessage();
        }
    }

    /** 清理超过保留周期的备份记录与文件 */
    public void cleanupExpired(int retentionDays) {
        if (retentionDays <= 0) return;
        LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);
        List<BackupRecord> expired = backupRecordMapper.selectList(new LambdaQueryWrapper<BackupRecord>()
                .lt(BackupRecord::getCreateTime, cutoff));
        for (BackupRecord r : expired) {
            if (r.getFilePath() != null) {
                try {
                    File f = new File(r.getFilePath());
                    if (f.exists()) f.delete();
                } catch (Exception ignored) {
                }
            }
            backupRecordMapper.deleteById(r.getId());
        }
        if (!expired.isEmpty()) {
            log.info("已清理过期备份 {} 条（保留 {} 天）", expired.size(), retentionDays);
        }
    }

    private String truncate(String s) {
        if (s == null) return "";
        return s.length() > 500 ? s.substring(0, 500) : s;
    }

    private String extractDbName(String url) {
        if (url == null) return "bms_db";
        int idx = url.lastIndexOf("/");
        if (idx < 0) return "bms_db";
        String tail = url.substring(idx + 1);
        int q = tail.indexOf("?");
        return q > 0 ? tail.substring(0, q) : tail;
    }
}
