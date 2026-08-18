package com.koolearn.bms.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.koolearn.bms.dto.InboundOrderDTO;
import com.koolearn.bms.entity.InRecord;
import com.koolearn.bms.entity.InStorageItem;
import com.koolearn.bms.entity.InboundOrder;
import com.koolearn.bms.entity.Material;
import com.koolearn.bms.mapper.InRecordMapper;
import com.koolearn.bms.service.InboundOrderService;
import com.koolearn.bms.service.MaterialService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 入库确认测试：
 * 1. 确认入库后库存增加
 * 2. InRecord 写入 locationNo + materialName + materialCode
 * 3. 物料编码自动生成
 * 4. 重复确认被拒绝
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class InboundConfirmStockTest {

    @Autowired
    private InboundOrderService inboundOrderService;

    @Autowired
    private MaterialService materialService;

    @Autowired
    private InRecordMapper inRecordMapper;

    private Material createMaterial(String code, BigDecimal stock) {
        Material mat = new Material();
        mat.setMaterialName("贴片电容");
        mat.setMaterialCode(code);
        mat.setStock(stock);
        mat.setLockStock(BigDecimal.ZERO);
        materialService.save(mat);
        return mat;
    }

    private InboundOrder createOrderAndConfirm(Material mat, BigDecimal num, String batchNo, String location) {
        InboundOrderDTO dto = new InboundOrderDTO();
        dto.setBillNo("RK-" + System.nanoTime());
        dto.setSupplier("测试供应商");
        dto.setUserName("测试申请人");
        dto.setInType("PURCHASE");
        InStorageItem item = new InStorageItem();
        item.setMaterialId(mat.getId());
        item.setNum(num);
        item.setBatchNo(batchNo);
        item.setLocationNo(location);
        dto.setItemList(Collections.singletonList(item));
        inboundOrderService.saveDraft(dto);

        InboundOrder order = inboundOrderService.list(new LambdaQueryWrapper<InboundOrder>()
                .eq(InboundOrder::getBillNo, dto.getBillNo())).get(0);
        assertEquals("PURCHASE", order.getInType(), "入库类型必须持久化");
        inboundOrderService.confirmIn(order.getId(), "库管员");
        return order;
    }

    @Test
    void confirmInIncreasesStockAndWritesRecord() {
        Material mat = createMaterial(null, BigDecimal.ZERO);
        InboundOrder order = createOrderAndConfirm(mat, new BigDecimal("300"), "2439", "A-01-01");

        Material updated = materialService.getById(mat.getId());
        assertEquals(0, new BigDecimal("300").compareTo(updated.getStock()), "库存应增加300");
        assertEquals("A-01-01", updated.getLocationNo(), "物料货位应更新");
        assertNotNull(updated.getMaterialCode(), "物料编码应自动生成");
        assertTrue(updated.getMaterialCode().startsWith("MTR-"), "物料编码应以MTR-开头");

        List<InRecord> records = inRecordMapper.selectList(new LambdaQueryWrapper<InRecord>()
                .eq(InRecord::getBillNo, order.getBillNo()));
        assertEquals(1, records.size());
        InRecord rec = records.get(0);
        assertEquals("贴片电容", rec.getMaterialName());
        assertEquals(updated.getMaterialCode(), rec.getMaterialCode());
        assertEquals("A-01-01", rec.getLocationNo());
        assertEquals("库管员", rec.getInUser());
        assertEquals(0, new BigDecimal("300").compareTo(rec.getInNum()));
    }

    @Test
    void duplicateConfirmRejected() {
        Material mat = createMaterial("MTR-TEST-1", BigDecimal.ZERO);
        InboundOrder order = createOrderAndConfirm(mat, new BigDecimal("50"), "B001", "B-01");
        assertThrows(RuntimeException.class, () -> inboundOrderService.confirmIn(order.getId(), "库管员"),
                "重复确认入库应被拒绝");
    }

    @Test
    void multipleItemsEachGetRecord() {
        Material mat1 = createMaterial("MTR-M1", BigDecimal.ZERO);
        Material mat2 = createMaterial("MTR-M2", BigDecimal.ZERO);
        InboundOrderDTO dto = new InboundOrderDTO();
        dto.setBillNo("RK-MULTI-" + System.nanoTime());
        dto.setSupplier("S");
        dto.setUserName("U");

        InStorageItem it1 = new InStorageItem();
        it1.setMaterialId(mat1.getId());
        it1.setNum(new BigDecimal("10"));
        it1.setBatchNo("B1");
        it1.setLocationNo("L1");
        InStorageItem it2 = new InStorageItem();
        it2.setMaterialId(mat2.getId());
        it2.setNum(new BigDecimal("20"));
        it2.setBatchNo("B2");
        it2.setLocationNo("L2");
        dto.setItemList(java.util.Arrays.asList(it1, it2));
        inboundOrderService.saveDraft(dto);

        InboundOrder order = inboundOrderService.list(new LambdaQueryWrapper<InboundOrder>()
                .eq(InboundOrder::getBillNo, dto.getBillNo())).get(0);
        inboundOrderService.confirmIn(order.getId(), "库管员");

        assertEquals(0, new BigDecimal("10").compareTo(materialService.getById(mat1.getId()).getStock()));
        assertEquals(0, new BigDecimal("20").compareTo(materialService.getById(mat2.getId()).getStock()));
        assertEquals(2, inRecordMapper.selectList(new LambdaQueryWrapper<InRecord>()
                .eq(InRecord::getBillNo, dto.getBillNo())).size());
    }
}
