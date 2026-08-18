package com.koolearn.bms.acceptance;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.koolearn.bms.dto.OutStorageItemDTO;
import com.koolearn.bms.dto.OutboundOrderDTO;
import com.koolearn.bms.entity.Material;
import com.koolearn.bms.entity.OutboundOrder;
import com.koolearn.bms.service.MaterialService;
import com.koolearn.bms.service.OutboundOrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/** Evaluator-owned inventory invariants, injected unchanged into every candidate. */
@SpringBootTest(properties = {
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.url=jdbc:h2:mem:evaluator_inventory;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.sql.init.mode=always"
})
@ActiveProfiles("test")
class ExternalInventoryAcceptanceTest {

    @Autowired private MaterialService materialService;
    @Autowired private OutboundOrderService outboundOrderService;

    private String suffix;

    @BeforeEach
    void uniqueSuffix() {
        suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    }

    @Test
    void duplicateConfirmationCannotDeductStockTwice() {
        Material material = material("dupconfirm", "100", "0");
        OutboundOrderDTO dto = outbound("dupconfirm", material, "30");
        outboundOrderService.saveDraft(dto);
        OutboundOrder order = order(dto.getOutboundCode());

        outboundOrderService.confirmOut(order.getId(), "验收库管");
        assertMoney("70", reload(material).getStock());
        assertMoney("0", reload(material).getLockStock());

        assertThrows(RuntimeException.class,
                () -> outboundOrderService.confirmOut(order.getId(), "验收库管"),
                "Confirming an already completed outbound order must be rejected");
        assertMoney("70", reload(material).getStock());
        assertMoney("0", reload(material).getLockStock());
    }

    @Test
    void editingDraftReplacesRatherThanAccumulatesItsStockLock() {
        Material material = material("editlock", "100", "0");
        OutboundOrderDTO dto = outbound("editlock", material, "30");
        outboundOrderService.saveDraft(dto);
        OutboundOrder order = order(dto.getOutboundCode());
        assertMoney("30", reload(material).getLockStock());

        OutboundOrderDTO edited = outbound("editlock", material, "10");
        edited.setId(order.getId());
        outboundOrderService.editDraft(order.getId(), edited);

        assertMoney("10", reload(material).getLockStock());
        assertMoney("100", reload(material).getStock());
    }

    @Test
    void negativeOutboundQuantityIsRejectedWithoutChangingInventory() {
        Material material = material("negative", "100", "0");
        OutboundOrderDTO dto = outbound("negative", material, "-5");

        assertThrows(RuntimeException.class, () -> outboundOrderService.saveDraft(dto),
                "Negative outbound quantity must be rejected");
        assertMoney("100", reload(material).getStock());
        assertMoney("0", reload(material).getLockStock());
        assertNull(outboundOrderService.getOne(new LambdaQueryWrapper<OutboundOrder>()
                .eq(OutboundOrder::getOutboundCode, dto.getOutboundCode())),
                "Rejected draft must not leave a persisted order");
    }

    @Test
    void duplicateMaterialLinesCannotOversubscribeStock() {
        Material material = material("duplines", "100", "0");
        OutboundOrderDTO dto = outbound("duplines", material, "60");
        OutStorageItemDTO second = item(material, "50");
        dto.setItemList(new java.util.ArrayList<>(dto.getItemList()));
        dto.getItemList().add(second);

        assertThrows(RuntimeException.class, () -> outboundOrderService.saveDraft(dto),
                "Duplicate lines must be validated against their combined requested quantity");
        assertMoney("100", reload(material).getStock());
        assertMoney("0", reload(material).getLockStock());
    }

    @Test
    void twoConcurrentDraftsCannotBothReserveTheSameStock() throws Exception {
        Material material = material("concurrent", "100", "0");
        OutboundOrderDTO first = outbound("concurrent_a", material, "60");
        OutboundOrderDTO second = outbound("concurrent_b", material, "60");

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger successes = new AtomicInteger();

        java.util.concurrent.Callable<Void> taskA = () -> {
            ready.countDown();
            start.await();
            try {
                outboundOrderService.saveDraft(first);
                successes.incrementAndGet();
            } catch (RuntimeException ignored) {
                // Exactly one contender is expected to lose.
            }
            return null;
        };
        java.util.concurrent.Callable<Void> taskB = () -> {
            ready.countDown();
            start.await();
            try {
                outboundOrderService.saveDraft(second);
                successes.incrementAndGet();
            } catch (RuntimeException ignored) {
                // Exactly one contender is expected to lose.
            }
            return null;
        };

        try {
            Future<Void> a = pool.submit(taskA);
            Future<Void> b = pool.submit(taskB);
            ready.await();
            start.countDown();
            a.get();
            b.get();
        } finally {
            pool.shutdownNow();
        }

        Material after = reload(material);
        assertEquals(1, successes.get(),
                "Only one of two concurrent 60-unit reservations against stock 100 may succeed");
        assertMoney("60", after.getLockStock());
        assertTrue(after.getStock().subtract(after.getLockStock()).signum() >= 0,
                "Available stock must never become negative");
    }

    private Material material(String label, String stock, String locked) {
        Material m = new Material();
        m.setMaterialCode("EXT-" + label + "-" + suffix);
        m.setMaterialName("外部验收物料-" + label);
        m.setPackageType("TEST");
        m.setSpecModel("TEST");
        m.setWarehouseCode("W1");
        m.setLocationNo("A-01");
        m.setStock(new BigDecimal(stock));
        m.setLockStock(new BigDecimal(locked));
        m.setMinStock(BigDecimal.ZERO);
        m.setMaxStock(new BigDecimal("1000"));
        m.setVersion(0);
        assertTrue(materialService.save(m));
        assertNotNull(m.getId());
        return m;
    }

    private OutboundOrderDTO outbound(String label, Material material, String amount) {
        OutboundOrderDTO dto = new OutboundOrderDTO();
        dto.setOutboundCode("EXT-OUT-" + label + "-" + suffix);
        dto.setOutType(1);
        dto.setApplyUser("external_" + label + "_" + suffix);
        dto.setRemark("evaluator hidden acceptance");
        dto.setItemList(Collections.singletonList(item(material, amount)));
        return dto;
    }

    private OutStorageItemDTO item(Material material, String amount) {
        OutStorageItemDTO item = new OutStorageItemDTO();
        item.setMaterialId(material.getId());
        item.setMaterialCode(material.getMaterialCode());
        item.setBatchNo("BATCH-" + suffix);
        item.setOutNum(new BigDecimal(amount));
        return item;
    }

    private OutboundOrder order(String code) {
        OutboundOrder order = outboundOrderService.getOne(new LambdaQueryWrapper<OutboundOrder>()
                .eq(OutboundOrder::getApplyUser, "external_" + code.substring(8, code.lastIndexOf('-')) + "_" + suffix)
                .orderByDesc(OutboundOrder::getId)
                .last("LIMIT 1"));
        assertNotNull(order, "Draft order must be persisted");
        return order;
    }

    private Material reload(Material material) {
        Material reloaded = materialService.getById(material.getId());
        assertNotNull(reloaded);
        return reloaded;
    }

    private void assertMoney(String expected, BigDecimal actual) {
        assertNotNull(actual);
        assertEquals(0, new BigDecimal(expected).compareTo(actual),
                "Expected " + expected + " but got " + actual);
    }
}
