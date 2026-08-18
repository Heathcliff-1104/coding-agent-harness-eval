package com.koolearn.bms.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.koolearn.bms.entity.SysOperationLog;
import com.koolearn.bms.entity.SysConfig;
import com.koolearn.bms.service.SysConfigService;
import com.koolearn.bms.service.SysOperationLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 系统维护定时任务：日志保留期自动清理。
 */
@Slf4j
@Component
public class SystemMaintenanceScheduler {

    private final SysOperationLogService sysOperationLogService;
    private final SysConfigService sysConfigService;

    public SystemMaintenanceScheduler(SysOperationLogService sysOperationLogService,
                                      SysConfigService sysConfigService) {
        this.sysOperationLogService = sysOperationLogService;
        this.sysConfigService = sysConfigService;
    }

    // 每日凌晨 3:30 执行日志清理
    @Scheduled(cron = "0 30 3 * * ?")
    public void cleanExpiredLogs() {
        try {
            int days;
            try {
                days = Integer.parseInt(sysConfigService.get("log.retention.days", "365"));
            } catch (NumberFormatException e) {
                days = 365;
            }
            LocalDateTime cutoff = LocalDateTime.now().minusDays(days);
            sysOperationLogService.remove(new LambdaQueryWrapper<SysOperationLog>()
                    .lt(SysOperationLog::getCreateTime, cutoff));
            log.info("已清理超过 {} 天的系统日志", days);
        } catch (Exception e) {
            log.error("日志清理任务异常", e);
        }
    }
}
