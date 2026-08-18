package com.koolearn.bms.config;

import com.koolearn.bms.entity.LoginLog;
import com.koolearn.bms.entity.SysOperationLog;
import com.koolearn.bms.mapper.LoginLogMapper;
import com.koolearn.bms.mapper.SysOperationLogMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 系统日志清理：每天凌晨 4:00 删除超过 sys.log.retention.days 天的登录日志与操作日志。
 */
@Slf4j
@Component
public class SysLogCleanupScheduler {

    private final LoginLogMapper loginLogMapper;
    private final SysOperationLogMapper sysOperationLogMapper;

    @Value("${sys.log.retention.days:365}")
    private int retentionDays;

    public SysLogCleanupScheduler(LoginLogMapper loginLogMapper, SysOperationLogMapper sysOperationLogMapper) {
        this.loginLogMapper = loginLogMapper;
        this.sysOperationLogMapper = sysOperationLogMapper;
    }

    @Scheduled(cron = "0 0 4 * * ?")
    public void cleanup() {
        try {
            LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);
            int login = loginLogMapper.delete(new LambdaQueryWrapper<LoginLog>().lt(LoginLog::getLoginTime, cutoff));
            int op = sysOperationLogMapper.delete(new LambdaQueryWrapper<SysOperationLog>().lt(SysOperationLog::getCreateTime, cutoff));
            if (login + op > 0) {
                log.info("系统日志清理完成: 登录日志{}条 操作日志{}条（保留{}天）", login, op, retentionDays);
            }
        } catch (Exception e) {
            log.warn("系统日志清理异常: {}", e.getMessage());
        }
    }
}
