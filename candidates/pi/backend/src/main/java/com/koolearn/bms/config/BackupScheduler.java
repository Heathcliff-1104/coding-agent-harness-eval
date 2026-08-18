package com.koolearn.bms.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.koolearn.bms.entity.BackupConfig;
import com.koolearn.bms.mapper.BackupConfigMapper;
import com.koolearn.bms.service.BackupService;
import com.koolearn.bms.service.impl.BackupServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 自动备份调度：
 * - 每周日凌晨 2:00 全量备份
 * - 每日凌晨 3:00 增量备份（简化：mysqldump 全表导出并标注为增量，见 BackupServiceImpl）
 * 按 backup_config 中 enabled 配置执行，并定期清理过期备份。
 */
@Slf4j
@Component
public class BackupScheduler {

    private final BackupService backupService;
    private final BackupConfigMapper backupConfigMapper;

    public BackupScheduler(BackupService backupService, BackupConfigMapper backupConfigMapper) {
        this.backupService = backupService;
        this.backupConfigMapper = backupConfigMapper;
    }

    @Scheduled(cron = "0 0 2 ? * SUN")
    public void weeklyFullBackup() {
        runScheduledBackup("full");
    }

    @Scheduled(cron = "0 0 3 * * ?")
    public void dailyIncrementalBackup() {
        runScheduledBackup("incremental");
    }

    @Scheduled(cron = "0 0 4 * * ?")
    public void dailyCleanup() {
        try {
            List<BackupConfig> configs = backupConfigMapper.selectList(
                    new LambdaQueryWrapper<BackupConfig>().eq(BackupConfig::getEnabled, 1));
            int days = 30;
            if (!configs.isEmpty() && configs.get(0).getRetentionDays() != null) {
                days = configs.get(0).getRetentionDays();
            }
            if (backupService instanceof BackupServiceImpl) {
                ((BackupServiceImpl) backupService).cleanupExpired(days);
            }
        } catch (Exception e) {
            log.warn("备份清理任务异常: {}", e.getMessage());
        }
    }

    private void runScheduledBackup(String type) {
        try {
            BackupConfig cfg = backupConfigMapper.selectOne(
                    new LambdaQueryWrapper<BackupConfig>()
                            .eq(BackupConfig::getBackupType, type)
                            .last("limit 1"));
            if (cfg == null || cfg.getEnabled() == null || cfg.getEnabled() != 1) {
                log.info("自动备份已禁用: type={}", type);
                return;
            }
            backupService.backup(type);
            cfg.setLastRun(LocalDateTime.now());
            backupConfigMapper.updateById(cfg);
        } catch (Exception e) {
            log.error("自动备份任务异常: type={} err={}", type, e.getMessage());
        }
    }
}
