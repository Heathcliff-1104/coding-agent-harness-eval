package com.koolearn.bms.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.koolearn.bms.controller.BackupController;
import com.koolearn.bms.entity.LoginLog;
import com.koolearn.bms.entity.SysOperationLog;
import com.koolearn.bms.mapper.LoginLogMapper;
import com.koolearn.bms.mapper.SysConfigMapper;
import com.koolearn.bms.mapper.SysOperationLogMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 系统日志保留期限（需求 2.6.4）：每日凌晨清理超过保留天数的操作日志与登录日志。
 * 保留天数可在系统配置 log.retention.days 调整（默认365天）。
 */
@Slf4j
@Component
public class LogRetentionScheduler {

    private final SysOperationLogMapper operationLogMapper;
    private final LoginLogMapper loginLogMapper;
    private final SysConfigMapper sysConfigMapper;

    public LogRetentionScheduler(SysOperationLogMapper operationLogMapper,
                                 LoginLogMapper loginLogMapper,
                                 SysConfigMapper sysConfigMapper) {
        this.operationLogMapper = operationLogMapper;
        this.loginLogMapper = loginLogMapper;
        this.sysConfigMapper = sysConfigMapper;
    }

    @Scheduled(cron = "0 30 3 * * ?")
    public void cleanExpiredLogs() {
        try {
            int days = Integer.parseInt(BackupController.getConfigValue(sysConfigMapper, "log.retention.days", "365"));
            LocalDateTime cutoff = LocalDateTime.now().minusDays(days);
            int op = operationLogMapper.delete(new LambdaQueryWrapper<SysOperationLog>().lt(SysOperationLog::getCreateTime, cutoff));
            int login = loginLogMapper.delete(new LambdaQueryWrapper<LoginLog>().lt(LoginLog::getLoginTime, cutoff));
            log.info("日志保留清理完成: 操作日志删除{}条, 登录日志删除{}条（保留{}天）", op, login, days);
        } catch (Exception e) {
            log.error("日志保留清理异常", e);
        }
    }
}
