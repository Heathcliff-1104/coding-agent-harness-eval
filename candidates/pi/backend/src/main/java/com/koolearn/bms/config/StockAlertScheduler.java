package com.koolearn.bms.config;

import com.koolearn.bms.service.StockAlertService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class StockAlertScheduler {

    private final StockAlertService stockAlertService;

    public StockAlertScheduler(StockAlertService stockAlertService) {
        this.stockAlertService = stockAlertService;
    }

    @Scheduled(cron = "0 0 2 * * ?")
    public void dailyScan() {
        log.info("开始每日库存预警扫描");
        try {
            stockAlertService.scanAndAlert();
            log.info("库存预警扫描完成");
        } catch (Exception e) {
            log.error("库存预警扫描异常", e);
        }
    }
}
