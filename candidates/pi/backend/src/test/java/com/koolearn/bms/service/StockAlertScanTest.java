package com.koolearn.bms.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.koolearn.bms.entity.Material;
import com.koolearn.bms.entity.StockAlert;
import com.koolearn.bms.mapper.StockAlertMapper;
import com.koolearn.bms.service.MaterialService;
import com.koolearn.bms.service.StockAlertService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 库存预警扫描测试：
 * 1. 低于最低库存 → 生成低库存预警(type=1)
 * 2. 同一天重复扫描不重复生成
 * 3. 超过最大库存 → 生成超储预警(type=2)
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class StockAlertScanTest {

    @Autowired
    private StockAlertService stockAlertService;

    @Autowired
    private StockAlertMapper stockAlertMapper;

    @Autowired
    private MaterialService materialService;

    private Material createMaterial(BigDecimal stock, BigDecimal min, BigDecimal max) {
        Material mat = new Material();
        mat.setMaterialName("预警测试物料");
        mat.setMaterialCode("MTR-ALERT-" + System.nanoTime());
        mat.setStock(stock);
        mat.setLockStock(BigDecimal.ZERO);
        mat.setMinStock(min);
        mat.setMaxStock(max);
        materialService.save(mat);
        return mat;
    }

    private List<StockAlert> alertsOf(Long materialId) {
        return stockAlertMapper.selectList(new LambdaQueryWrapper<StockAlert>()
                .eq(StockAlert::getMaterialId, materialId));
    }

    @Test
    void lowStockAlertCreatedAndNotDuplicatedSameDay() {
        Material mat = createMaterial(new BigDecimal("5"), new BigDecimal("10"), new BigDecimal("100"));
        stockAlertService.scanAndAlert();
        assertEquals(1, alertsOf(mat.getId()).size());
        assertEquals(1, alertsOf(mat.getId()).get(0).getAlertType(), "低库存预警类型应为1");
        assertEquals(0, new BigDecimal("10").compareTo(alertsOf(mat.getId()).get(0).getThresholdStock()));

        // 同一天再次扫描：不重复生成
        stockAlertService.scanAndAlert();
        assertEquals(1, alertsOf(mat.getId()).size(), "同一天不应重复生成预警");
    }

    @Test
    void overMaxStockCreatesType2Alert() {
        Material mat = createMaterial(new BigDecimal("500"), new BigDecimal("10"), new BigDecimal("100"));
        stockAlertService.scanAndAlert();
        List<StockAlert> alerts = alertsOf(mat.getId());
        assertEquals(1, alerts.size());
        assertEquals(2, alerts.get(0).getAlertType(), "超储预警类型应为2");
        assertEquals(0, new BigDecimal("100").compareTo(alerts.get(0).getThresholdStock()));
    }

    @Test
    void normalStockNoAlert() {
        Material mat = createMaterial(new BigDecimal("50"), new BigDecimal("10"), new BigDecimal("100"));
        stockAlertService.scanAndAlert();
        assertEquals(0, alertsOf(mat.getId()).size());
    }

    @Test
    void handledAlertNotRecreatedSameDay() {
        Material mat = createMaterial(new BigDecimal("5"), new BigDecimal("10"), new BigDecimal("100"));
        stockAlertService.scanAndAlert();
        List<StockAlert> alerts = alertsOf(mat.getId());
        assertEquals(1, alerts.size());

        // 标记已处理（处理人/方式）
        StockAlert alert = alerts.get(0);
        alert.setHandled(1);
        alert.setHandler("库管员");
        alert.setHandleMethod("已采购");
        stockAlertMapper.updateById(alert);

        // 同一天再次扫描：不应因 handled=1 而重新生成（按物料+类型+日期去重，与 handled 无关）
        stockAlertService.scanAndAlert();
        assertEquals(1, alertsOf(mat.getId()).size(), "标记处理后当天不应重复告警");
        assertEquals(1, alertsOf(mat.getId()).get(0).getHandled(), "原记录仍保持已处理状态");
    }
}
