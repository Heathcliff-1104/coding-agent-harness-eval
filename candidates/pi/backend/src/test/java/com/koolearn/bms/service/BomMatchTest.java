package com.koolearn.bms.service;

import com.koolearn.bms.dto.OutboundOrderDTO;
import com.koolearn.bms.dto.OutStorageItemDTO;
import com.koolearn.bms.entity.Material;
import com.koolearn.bms.service.BomMatchService;
import com.koolearn.bms.service.MaterialService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * BOM 匹配状态测试：
 * 充足 / 不足 / 缺料 / 被占用 四种状态判定正确
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class BomMatchTest {

    @Autowired
    private BomMatchService bomMatchService;

    @Autowired
    private MaterialService materialService;

    private Material createMaterial(String code, BigDecimal stock, BigDecimal lockStock) {
        Material mat = new Material();
        mat.setMaterialCode(code);
        mat.setMaterialName("电容 " + code);
        mat.setPackageType("0603");
        mat.setStock(stock);
        mat.setLockStock(lockStock != null ? lockStock : BigDecimal.ZERO);
        materialService.save(mat);
        return mat;
    }

    private Map<String, Object> matchItem(String code, BigDecimal need) {
        Map<String, Object> item = new java.util.HashMap<>();
        item.put("materialCode", code);
        item.put("materialName", "电容 " + code);
        item.put("packageType", "0603");
        item.put("specModel", "CBR06C4");
        item.put("batchNo", "2439");
        item.put("needNum", need);
        return item;
    }

    @Test
    void matchReturnsCorrectStatuses() {
        createMaterial("R1", new BigDecimal("100"), BigDecimal.ZERO);  // 充足
        createMaterial("R2", new BigDecimal("30"), BigDecimal.ZERO);   // 不足 (30 < 50)
        createMaterial("R3", BigDecimal.ZERO, BigDecimal.ZERO);        // 缺料 (库存0)
        createMaterial("R4", new BigDecimal("10"), new BigDecimal("10")); // 被占用 (可用0但库存>0)

        List<Map<String, Object>> items = Arrays.asList(
                matchItem("R1", new BigDecimal("50")),
                matchItem("R2", new BigDecimal("50")),
                matchItem("R3", new BigDecimal("10")),
                matchItem("R4", new BigDecimal("5"))
        );
        List<Map<String, Object>> result = bomMatchService.match(items);

        assertEquals(4, result.size());
        assertEquals("充足", result.get(0).get("status"));
        assertEquals("不足", result.get(1).get("status"));
        assertEquals("缺料", result.get(2).get("status"));
        assertEquals("被占用", result.get(3).get("status"));

        // 补充数量 = max(0, need - available)
        assertEquals(0, new BigDecimal("20").compareTo((BigDecimal) result.get(1).get("shortage")));
        assertEquals(0, new BigDecimal("5").compareTo((BigDecimal) result.get(3).get("shortage")));
    }

    @Test
    void unknownMaterialIsOutOfStock() {
        List<Map<String, Object>> items = new ArrayList<>();
        items.add(matchItem("NOT-EXIST-999", new BigDecimal("10")));
        List<Map<String, Object>> result = bomMatchService.match(items);
        assertEquals("缺料", result.get(0).get("status"));
    }
}
