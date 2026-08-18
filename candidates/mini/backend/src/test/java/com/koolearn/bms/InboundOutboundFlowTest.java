package com.koolearn.bms;

import com.koolearn.bms.dto.InboundOrderDTO;
import com.koolearn.bms.dto.OutboundOrderDTO;
import com.koolearn.bms.dto.OutStorageItemDTO;
import com.koolearn.bms.entity.InStorageItem;
import com.koolearn.bms.entity.Material;
import com.koolearn.bms.entity.OutboundOrder;
import com.koolearn.bms.service.InboundOrderService;
import com.koolearn.bms.service.MaterialService;
import com.koolearn.bms.service.OutboundOrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.url=jdbc:h2:mem:bms;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.sql.init.mode=always",
        "bms.jwt.secret=test-jwt-secret-0123456789-0123456789-0123456789"
})
class InboundOutboundFlowTest {

    @Autowired
    private MaterialService materialService;

    @Autowired
    private InboundOrderService inboundOrderService;

    @Autowired
    private OutboundOrderService outboundOrderService;

    private Material createMaterial(String name, BigDecimal stock) {
        Material m = new Material();
        m.setMaterialName(name);
        m.setMaterialCode("M-" + System.nanoTime());
        m.setStock(stock);
        m.setLockStock(BigDecimal.ZERO);
        m.setVersion(0);
        materialService.save(m);
        return m;
    }

    @Test
    void inboundConfirmIncreasesStockAndWritesRecord() {
        Material m = createMaterial("电阻-100", new BigDecimal("0"));
        InboundOrderDTO dto = new InboundOrderDTO();
        dto.setBillNo("IN" + System.nanoTime());
        dto.setSupplier("供应商A");
        dto.setUserName("tester");
        InStorageItem item = new InStorageItem();
        item.setMaterialId(m.getId());
        item.setMaterialCode(m.getMaterialCode());
        item.setBatchNo("B1");
        item.setNum(new BigDecimal("50"));
        dto.setItemList(Collections.singletonList(item));

        inboundOrderService.saveOrder(dto);
        // find order by billNo
        com.koolearn.bms.entity.InboundOrder order = inboundOrderService.getOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.koolearn.bms.entity.InboundOrder>()
                        .eq(com.koolearn.bms.entity.InboundOrder::getBillNo, dto.getBillNo()));
        assertThat(order).isNotNull();
        inboundOrderService.confirmIn(order.getId(), "warehouse_user");

        Material after = materialService.getById(m.getId());
        assertThat(after.getStock()).isEqualByComparingTo("50");
    }

    @Test
    void outboundSaveOrderWithoutDraftCreatesOrderAndConfirmationReducesStock() {
        Material m = createMaterial("电容-200", new BigDecimal("100"));
        OutboundOrderDTO dto = new OutboundOrderDTO();
        dto.setOutType(1);
        dto.setApplyUser("engineer_user");
        dto.setRemark("生产领料");
        OutStorageItemDTO item = new OutStorageItemDTO();
        item.setMaterialId(m.getId());
        item.setMaterialCode(m.getMaterialCode());
        item.setOutNum(new BigDecimal("30"));
        dto.setItemList(Collections.singletonList(item));

        // 不提供 id，验证 saveOrder 自动创建草稿并锁定库存
        outboundOrderService.saveOrder(dto);

        Material locked = materialService.getById(m.getId());
        assertThat(locked.getLockStock()).isEqualByComparingTo("30");

        // 查找最新出库单
        OutboundOrder order = outboundOrderService.getOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<OutboundOrder>()
                        .eq(OutboundOrder::getApplyUser, "engineer_user")
                        .orderByDesc(OutboundOrder::getCreateTime)
                        .last("limit 1"));
        assertThat(order).isNotNull();
        assertThat(order.getOrderStatus()).isEqualTo(0);

        // 确认出库
        outboundOrderService.confirmOut(order.getId(), "warehouse_user");

        Material after = materialService.getById(m.getId());
        assertThat(after.getStock()).isEqualByComparingTo("70");
        assertThat(after.getLockStock()).isEqualByComparingTo("0");
    }

    @Test
    void outboundRejectUnlocksStock() {
        Material m = createMaterial("芯片-300", new BigDecimal("60"));
        OutboundOrderDTO dto = new OutboundOrderDTO();
        dto.setOutType(1);
        dto.setApplyUser("engineer_user2");
        dto.setRemark("测试驳回");
        OutStorageItemDTO item = new OutStorageItemDTO();
        item.setMaterialId(m.getId());
        item.setOutNum(new BigDecimal("20"));
        dto.setItemList(Collections.singletonList(item));
        outboundOrderService.saveOrder(dto);

        Material locked = materialService.getById(m.getId());
        assertThat(locked.getLockStock()).isEqualByComparingTo("20");

        OutboundOrder order = outboundOrderService.getOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<OutboundOrder>()
                        .eq(OutboundOrder::getApplyUser, "engineer_user2")
                        .orderByDesc(OutboundOrder::getCreateTime)
                        .last("limit 1"));
        outboundOrderService.rejectOut(order.getId());

        Material after = materialService.getById(m.getId());
        assertThat(after.getLockStock()).isEqualByComparingTo("0");
        assertThat(after.getStock()).isEqualByComparingTo("60");
    }
}
