package com.koolearn.bms.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.koolearn.bms.dto.OutboundOrderDTO;
import com.koolearn.bms.dto.OutStorageItemDTO;
import com.koolearn.bms.entity.Material;
import com.koolearn.bms.entity.OutRecord;
import com.koolearn.bms.entity.OutboundOrder;
import com.koolearn.bms.mapper.OutRecordMapper;
import com.koolearn.bms.service.MaterialService;
import com.koolearn.bms.service.OutboundOrderService;
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
 * 出库锁定/确认/驳回测试：
 * 1. saveDraft 锁定库存（stock 不变、lock_stock 增加）
 * 2. 可用库存不足时抛异常
 * 3. confirmOut 扣减库存 + 释放锁定 + 写 OutRecord
 * 4. rejectOut 释放锁定
 * 5. 多单共同预订同一物料均可确认（确认时只看总库存，lockStock 已含本单占用）
 * 6. 总库存真正不足时才拦截
 * 7. 非草稿状态不允许编辑
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OutboundLockConfirmRejectTest {

    @Autowired
    private OutboundOrderService outboundOrderService;

    @Autowired
    private MaterialService materialService;

    @Autowired
    private OutRecordMapper outRecordMapper;

    private Material createMaterial(BigDecimal stock, BigDecimal lockStock) {
        Material mat = new Material();
        mat.setMaterialName("贴片电阻");
        mat.setMaterialCode("MTR-RES-1");
        mat.setStock(stock);
        mat.setLockStock(lockStock != null ? lockStock : BigDecimal.ZERO);
        mat.setMinStock(new BigDecimal("5"));
        materialService.save(mat);
        return mat;
    }

    private Long saveDraftWithNum(Material mat, BigDecimal num) {
        OutboundOrderDTO dto = new OutboundOrderDTO();
        dto.setOutType(1);
        dto.setApplyUser("工程师A");
        OutStorageItemDTO item = new OutStorageItemDTO();
        item.setMaterialId(mat.getId());
        item.setMaterialCode(mat.getMaterialCode());
        item.setBatchNo("2439");
        item.setOutNum(num);
        dto.setItemList(Collections.singletonList(item));
        return outboundOrderService.saveDraft(dto);
    }

    @Test
    void saveDraftLocksStock() {
        Material mat = createMaterial(new BigDecimal("100"), BigDecimal.ZERO);
        Long orderId = saveDraftWithNum(mat, new BigDecimal("30"));

        Material locked = materialService.getById(mat.getId());
        assertEquals(0, new BigDecimal("30").compareTo(locked.getLockStock()), "锁定库存应为30");
        assertEquals(0, new BigDecimal("100").compareTo(locked.getStock()), "库存不应变化");

        OutboundOrder order = outboundOrderService.getById(orderId);
        assertEquals(0, order.getOrderStatus());
    }

    @Test
    void saveDraftFailsWhenAvailabilityInsufficient() {
        Material mat = createMaterial(new BigDecimal("10"), new BigDecimal("5"));
        // 可用 = 10 - 5 = 5 < 8
        assertThrows(RuntimeException.class, () -> saveDraftWithNum(mat, new BigDecimal("8")),
                "可用库存不足时应抛出异常");
        Material after = materialService.getById(mat.getId());
        assertEquals(0, new BigDecimal("5").compareTo(after.getLockStock()), "失败时不应新增锁定");
    }

    @Test
    void confirmOutDecrementsUnlocksAndWritesRecord() {
        Material mat = createMaterial(new BigDecimal("100"), BigDecimal.ZERO);
        Long orderId = saveDraftWithNum(mat, new BigDecimal("30"));
        outboundOrderService.confirmOut(orderId, "库管员B");

        Material after = materialService.getById(mat.getId());
        assertEquals(0, new BigDecimal("70").compareTo(after.getStock()), "确认出库后库存应减30");
        assertEquals(0, BigDecimal.ZERO.compareTo(after.getLockStock()), "锁定应全部释放");

        OutboundOrder order = outboundOrderService.getById(orderId);
        assertEquals(1, order.getOrderStatus(), "状态应为已出库");
        assertEquals("库管员B", order.getOperUser());

        List<OutRecord> records = outRecordMapper.selectList(new LambdaQueryWrapper<OutRecord>()
                .eq(OutRecord::getOutboundCode, order.getOutboundCode()));
        assertEquals(1, records.size());
        assertEquals(0, new BigDecimal("30").compareTo(records.get(0).getOutNum()));
        assertEquals(mat.getId(), records.get(0).getMaterialId());
    }

    @Test
    void rejectOutUnlocksStock() {
        Material mat = createMaterial(new BigDecimal("100"), BigDecimal.ZERO);
        Long orderId = saveDraftWithNum(mat, new BigDecimal("30"));
        outboundOrderService.rejectOut(orderId);

        Material after = materialService.getById(mat.getId());
        assertEquals(0, BigDecimal.ZERO.compareTo(after.getLockStock()), "驳回后锁定应释放");
        assertEquals(0, new BigDecimal("100").compareTo(after.getStock()), "驳回不影响库存");

        OutboundOrder order = outboundOrderService.getById(orderId);
        assertEquals(2, order.getOrderStatus(), "状态应为已驳回");
    }

    @Test
    void confirmOutSucceedsWhenOwnLockPresentButStockCoversOutNum() {
        Material mat = createMaterial(new BigDecimal("50"), BigDecimal.ZERO);
        Long orderId = saveDraftWithNum(mat, new BigDecimal("40"));
        // 模拟另一张单也占用了部分库存：lock_stock 增加（含本单自己的 40 占用）
        Material m2 = materialService.getById(mat.getId());
        m2.setLockStock(m2.getLockStock().add(new BigDecimal("15")));
        materialService.updateById(m2);

        // 确认时只看总库存：50 >= 40，应可确认（lockStock 已包含本单占用，不能按可用=stock-lockStock 判断）
        outboundOrderService.confirmOut(orderId, "库管员");

        Material after = materialService.getById(mat.getId());
        assertEquals(0, new BigDecimal("10").compareTo(after.getStock()), "确认出库后库存应减40");
        assertEquals(0, new BigDecimal("15").compareTo(after.getLockStock()), "应只释放本单的40占用，其余单占用保留");
    }

    @Test
    void confirmOutFailsWhenStockGenuinelyInsufficient() {
        Material mat = createMaterial(new BigDecimal("50"), BigDecimal.ZERO);
        Long orderId = saveDraftWithNum(mat, new BigDecimal("40"));
        // 模拟总库存被真实消耗（非占用）：总库存降到 30 < 40
        Material m2 = materialService.getById(mat.getId());
        m2.setStock(new BigDecimal("30"));
        materialService.updateById(m2);

        assertThrows(RuntimeException.class, () -> outboundOrderService.confirmOut(orderId, "库管员"),
                "总库存不足时应抛异常");
        Material after = materialService.getById(mat.getId());
        assertEquals(0, new BigDecimal("40").compareTo(after.getLockStock()), "确认失败不应释放锁定");
        assertEquals(0, new BigDecimal("30").compareTo(after.getStock()), "确认失败不应扣减库存");
        // 状态回滚由事务保证：confirmOut 抛异常后整个事务回滚，状态恢复为待审批(0)
    }

    @Test
    void twoDraftsOnSameMaterialCanBothConfirm() {
        Material mat = createMaterial(new BigDecimal("100"), BigDecimal.ZERO);
        Long orderA = saveDraftWithNum(mat, new BigDecimal("40"));
        Long orderB = saveDraftWithNum(mat, new BigDecimal("30"));

        Material locked = materialService.getById(mat.getId());
        assertEquals(0, new BigDecimal("70").compareTo(locked.getLockStock()), "两张单应共同占用70");

        outboundOrderService.confirmOut(orderA, "库管员");
        Material afterA = materialService.getById(mat.getId());
        assertEquals(0, new BigDecimal("60").compareTo(afterA.getStock()));
        assertEquals(0, new BigDecimal("30").compareTo(afterA.getLockStock()), "A确认后剩余B的占用30");

        outboundOrderService.confirmOut(orderB, "库管员");
        Material afterB = materialService.getById(mat.getId());
        assertEquals(0, new BigDecimal("30").compareTo(afterB.getStock()));
        assertEquals(0, BigDecimal.ZERO.compareTo(afterB.getLockStock()), "B确认后占用全部释放");
    }

    @Test
    void editDraftRejectedWhenNotDraftStatus() {
        Material mat = createMaterial(new BigDecimal("100"), BigDecimal.ZERO);
        Long orderId = saveDraftWithNum(mat, new BigDecimal("30"));
        outboundOrderService.confirmOut(orderId, "库管员"); // 状态 -> 1 已出库

        OutboundOrderDTO dto = new OutboundOrderDTO();
        dto.setOutType(1);
        dto.setApplyUser("工程师A");
        OutStorageItemDTO item = new OutStorageItemDTO();
        item.setMaterialId(mat.getId());
        item.setMaterialCode(mat.getMaterialCode());
        item.setBatchNo("2439");
        item.setOutNum(new BigDecimal("10"));
        dto.setItemList(Collections.singletonList(item));

        assertThrows(RuntimeException.class, () -> outboundOrderService.editDraft(orderId, dto),
                "已出库单据不允许编辑");
    }
}
