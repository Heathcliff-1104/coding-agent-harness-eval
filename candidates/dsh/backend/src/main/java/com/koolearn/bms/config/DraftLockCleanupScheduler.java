package com.koolearn.bms.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.koolearn.bms.controller.BackupController;
import com.koolearn.bms.entity.OutboundOrder;
import com.koolearn.bms.mapper.OutboundOrderMapper;
import com.koolearn.bms.mapper.SysConfigMapper;
import com.koolearn.bms.service.OutboundOrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 草稿占用库存清理（需求安全项 H5）：
 * 超过 N 天（默认7天，可配置 outbound.draft.ttl.days）仍未提交/确认/驳回的出库草稿自动驳回并释放占用库存，
 * 防止永久锁定物料库存。
 */
@Slf4j
@Component
public class DraftLockCleanupScheduler {

    private final OutboundOrderService outboundOrderService;
    private final OutboundOrderMapper outboundOrderMapper;
    private final SysConfigMapper sysConfigMapper;

    public DraftLockCleanupScheduler(OutboundOrderService outboundOrderService,
                                     OutboundOrderMapper outboundOrderMapper,
                                     SysConfigMapper sysConfigMapper) {
        this.outboundOrderService = outboundOrderService;
        this.outboundOrderMapper = outboundOrderMapper;
        this.sysConfigMapper = sysConfigMapper;
    }

    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanStaleDrafts() {
        try {
            int ttlDays;
            try {
                ttlDays = Integer.parseInt(BackupController.getConfigValue(sysConfigMapper, "outbound.draft.ttl.days", "7"));
            } catch (NumberFormatException e) {
                ttlDays = 7;
            }
            LocalDateTime cutoff = LocalDateTime.now().minusDays(ttlDays);
            List<OutboundOrder> stale = outboundOrderMapper.selectList(new LambdaQueryWrapper<OutboundOrder>()
                    .eq(OutboundOrder::getOrderStatus, 0)
                    .lt(OutboundOrder::getCreateTime, cutoff)
                    .last("limit 200"));
            int ok = 0;
            for (OutboundOrder order : stale) {
                try {
                    outboundOrderService.rejectOut(order.getId());
                    ok++;
                } catch (Exception e) {
                    log.warn("清理过期草稿失败 id={}: {}", order.getId(), e.getMessage());
                }
            }
            if (ok > 0) {
                log.info("过期出库草稿自动驳回并释放占用: {} 单", ok);
            }
        } catch (Exception e) {
            log.error("草稿占用清理异常", e);
        }
    }
}
