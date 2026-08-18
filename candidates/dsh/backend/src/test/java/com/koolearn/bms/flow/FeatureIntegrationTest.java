package com.koolearn.bms.flow;

import com.koolearn.bms.entity.CisSyncLog;
import com.koolearn.bms.entity.Material;
import com.koolearn.bms.entity.StockAlert;
import com.koolearn.bms.mapper.CisSyncLogMapper;
import com.koolearn.bms.mapper.MaterialMapper;
import com.koolearn.bms.mapper.StockAlertMapper;
import com.koolearn.bms.service.BomPlanService;
import com.koolearn.bms.service.CisSyncService;
import com.koolearn.bms.service.MaterialService;
import com.koolearn.bms.service.StockAlertService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * BOM匹配、库存预警、CIS同步（演示模式）集成测试。
 */
@SpringBootTest
@ActiveProfiles("test")
class FeatureIntegrationTest {

    @Autowired private BomPlanService bomPlanService;
    @Autowired private StockAlertService stockAlertService;
    @Autowired private CisSyncService cisSyncService;
    @Autowired private MaterialService materialService;
    @Autowired private MaterialMapper materialMapper;
    @Autowired private StockAlertMapper stockAlertMapper;
    @Autowired private CisSyncLogMapper cisSyncLogMapper;

    @Test
    void bomMatchClassifiesStockStatus() {
        Material enough = new Material();
        enough.setMaterialName("物料A");
        enough.setStock(new BigDecimal("100"));
        enough.setLockStock(BigDecimal.ZERO);
        materialService.save(enough);

        Material none = new Material();
        none.setMaterialName("物料B");
        none.setStock(BigDecimal.ZERO);
        none.setLockStock(BigDecimal.ZERO);
        materialService.save(none);

        List<Map<String, Object>> items = new ArrayList<>();
        items.add(row("物料A", "10"));
        items.add(row("物料B", "5"));
        items.add(row("不存在物料X", "3"));

        List<Map<String, Object>> matched = bomPlanService.matchBom(items);
        assertEquals(3, matched.size());
        assertEquals("sufficient", matched.get(0).get("stockStatus"));
        assertEquals("out_of_stock", matched.get(1).get("stockStatus"));
        assertEquals("out_of_stock", matched.get(2).get("stockStatus"));
    }

    @Test
    void bomPlanSavedWithVersion() {
        List<Map<String, Object>> items = new ArrayList<>();
        items.add(row("物料A", "5"));
        String planNo = bomPlanService.savePlan("V1.0", bomPlanService.matchBom(items), "eng01");
        assertNotNull(planNo);
        assertTrue(planNo.startsWith("PLAN-"));
    }

    @Test
    void stockAlertScanCreatesLowStockAlert() {
        Material low = new Material();
        low.setMaterialName("低库存物料");
        low.setStock(new BigDecimal("2"));
        low.setMinStock(new BigDecimal("5"));
        materialService.save(low);

        stockAlertService.scanAndAlert();
        Long cnt = stockAlertMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<StockAlert>()
                        .eq(StockAlert::getMaterialId, low.getId())
                        .eq(StockAlert::getAlertType, 1));
        assertEquals(1L, cnt, "应生成一条低库存预警");

        // 同日再次扫描不重复生成
        stockAlertService.scanAndAlert();
        Long cnt2 = stockAlertMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<StockAlert>()
                        .eq(StockAlert::getMaterialId, low.getId())
                        .eq(StockAlert::getAlertType, 1)
                        .eq(StockAlert::getHandled, 0));
        assertEquals(1L, cnt2, "同日未处理预警不应重复生成");
    }

    @Test
    void cisFullSyncWritesLogInDemoMode() {
        String result = cisSyncService.syncFull();
        assertNotNull(result);
        assertTrue(result.contains("同步成功"));
        Long cnt = cisSyncLogMapper.selectCount(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<CisSyncLog>()
                .eq(CisSyncLog::getSyncType, "full"));
        assertEquals(1L, cnt, "全量同步应写入一条同步日志");
    }

    private Map<String, Object> row(String name, String needNum) {
        Map<String, Object> m = new HashMap<>();
        m.put("materialName", name);
        m.put("needNum", new BigDecimal(needNum));
        return m;
    }
}
