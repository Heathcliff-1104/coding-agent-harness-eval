package com.koolearn.bms.config;

import com.koolearn.bms.service.BackupService;
import com.koolearn.bms.service.SysConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 自动备份调度：
 *  - 每周日 02:00 全量备份（cron 可配置，默认 0 0 2 * * SUN）
 *  - 每日 02:30 增量备份（可配置开关）
 *  - 每日 03:00 清理过期备份（保留期可配置）
 */
@Slf4j
@Component
public class BackupScheduler {

    private final BackupService backupService;
    private final SysConfigService sysConfigService;

    public BackupScheduler(BackupService backupService, SysConfigService sysConfigService) {
        this.backupService = backupService;
        this.sysConfigService = sysConfigService;
    }

    @Scheduled(cron = "0 0 2 * * SUN")
    public void weeklyFullBackup() {
        try {
            backupService.backup("full");
        } catch (Exception e) {
            log.error("定时全量备份失败", e);
        }
    }

    @Scheduled(cron = "0 30 2 * * ?")
    public void dailyIncrementalBackup() {
        try {
            if ("true".equalsIgnoreCase(sysConfigService.get("backup.incremental.enabled", "true"))) {
                backupService.backup("incremental");
            }
        } catch (Exception e) {
            log.error("定时增量备份失败", e);
        }
    }

    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanupExpiredBackups() {
        try {
            int days = Integer.parseInt(sysConfigService.get("backup.retention.days", "30"));
            backupService.cleanupExpired(days);
        } catch (Exception e) {
            log.error("备份清理失败", e);
        }
    }
}
