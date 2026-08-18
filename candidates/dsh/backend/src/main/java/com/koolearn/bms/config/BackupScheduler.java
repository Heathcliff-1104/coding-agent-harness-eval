package com.koolearn.bms.config;

import com.koolearn.bms.controller.BackupController;
import com.koolearn.bms.mapper.SysConfigMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 自动备份调度（需求 2.6.3）：
 * - 每周日凌晨2点全量备份（cron 可在系统配置 backup.full.cron 调整）
 * - 每日凌晨2点增量备份（cron 可在系统配置 backup.incr.cron 调整）
 * - 备份文件保留周期见 backup.retention（默认30天），备份后自动清理
 */
@Slf4j
@Component
public class BackupScheduler {

    private final BackupController backupController;
    private final SysConfigMapper sysConfigMapper;

    public BackupScheduler(BackupController backupController, SysConfigMapper sysConfigMapper) {
        this.backupController = backupController;
        this.sysConfigMapper = sysConfigMapper;
    }

    @Scheduled(cron = "${backup.full.cron:0 0 2 * * 0}")
    public void fullBackup() {
        try {
            String result = backupController.doFullBackup();
            log.info("自动全量备份完成: {}", result);
        } catch (Exception e) {
            log.error("自动全量备份失败", e);
        }
    }

    @Scheduled(cron = "${backup.incr.cron:0 30 2 * * ?}")
    public void incrementalBackup() {
        try {
            String result = backupController.doIncrementalBackup();
            log.info("自动增量备份完成: {}", result);
        } catch (Exception e) {
            log.error("自动增量备份失败", e);
        }
    }
}
