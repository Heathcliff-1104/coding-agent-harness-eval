package com.koolearn.bms.flow;

import com.koolearn.bms.dto.InboundOrderDTO;
import com.koolearn.bms.dto.OutboundOrderDTO;
import com.koolearn.bms.dto.OutStorageItemDTO;
import com.koolearn.bms.entity.InRecord;
import com.koolearn.bms.entity.InStorageItem;
import com.koolearn.bms.entity.InboundOrder;
import com.koolearn.bms.entity.Material;
import com.koolearn.bms.entity.OutRecord;
import com.koolearn.bms.entity.OutboundOrder;
import com.koolearn.bms.entity.User;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.koolearn.bms.mapper.InRecordMapper;
import com.koolearn.bms.mapper.MaterialMapper;
import com.koolearn.bms.mapper.OutRecordMapper;
import com.koolearn.bms.service.InboundOrderService;
import com.koolearn.bms.service.MaterialService;
import com.koolearn.bms.service.OutboundOrderService;
import com.koolearn.bms.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 核心物料流集成测试（H2 内存库）：
 * 入库确认加库存/建档、出库占用/扣减/解锁、库存不足拦截、注册校验。
 */
@SpringBootTest
@ActiveProfiles("test")
class StockFlowIntegrationTest {

    @Autowired
    private InboundOrderService inboundOrderService;
    @Autowired
    private OutboundOrderService outboundOrderService;
    @Autowired
    private MaterialService materialService;
    @Autowired
    private MaterialMapper materialMapper;
    @Autowired
    private InRecordMapper inRecordMapper;
    @Autowired
    private OutRecordMapper outRecordMapper;
    @Autowired
    private UserService userService;

    @Test
    void registerRejectsWeakPasswordAndForcesEngineerRole() {
        User u = new User();
        u.setUsername("reg_test_1");
        u.setPassword("weak");
        u.setRealName("测试");
        u.setPhone("13900001111");
        u.setDept("硬件部");
        u.setRole("admin"); // 尝试越权
        RuntimeException ex = assertThrows(RuntimeException.class, () -> userService.register(u));
        assertTrue(ex.getMessage().contains("8~20") || ex.getMessage().contains("至少3类"));

        u.setPassword("Abcdef123");
        userService.register(u);
        User saved = userService.getOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>()
                .eq(User::getUsername, "reg_test_1"));
        assertEquals("engineer", saved.getRole(), "注册角色必须强制为engineer");
    }

    @Test
    void inboundConfirmCreatesMaterialAddsStockAndWritesRecord() {
        // 手动新建入库单：明细无 materialId（新物料），应自动建档并加库存
        InboundOrderDTO dto = new InboundOrderDTO();
        dto.setBillNo("RK-2026-0001");
        dto.setSupplier("测试供应商");
        dto.setUserName("wh01");
        InStorageItem item = new InStorageItem();
        item.setMaterialName("贴片电容");
        item.setPackageType("0603");
        item.setValueData("C0603_4.3pF");
        item.setSpecModel("CBR06C4");
        item.setBatchNo("2439");
        item.setNum(new BigDecimal("300"));
        item.setLocationNo("A-01-01");
        dto.setItemList(Collections.singletonList(item));

        inboundOrderService.saveOrder(dto);
        InboundOrder order = inboundOrderService.getOne(new LambdaQueryWrapper<InboundOrder>()
                .eq(InboundOrder::getBillNo, "RK-2026-0001"));
        assertNotNull(order, "入库单应保存成功");

        inboundOrderService.confirmIn(order.getId(), "wh01");

        // 物料已自动创建并编码
        Material mat = materialMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Material>()
                .eq(Material::getMaterialName, "贴片电容").last("limit 1"));
        assertNotNull(mat, "新物料应自动建档");
        assertNotNull(mat.getMaterialCode(), "物料编码应自动生成");
        assertEquals(0, mat.getStock().compareTo(new BigDecimal("300")), "库存应增加300");

        // 入库记录已写入
        Long cnt = inRecordMapper.selectCount(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<InRecord>()
                .eq(InRecord::getBillNo, "RK-2026-0001"));
        assertEquals(1L, cnt, "应写入一条入库记录");

        // 重复确认应被拦截
        RuntimeException ex = assertThrows(RuntimeException.class, () -> inboundOrderService.confirmIn(order.getId(), "wh01"));
        assertTrue(ex.getMessage().contains("重复"));

        // 入库单列表支持按明细物料名称/时间范围筛选
        com.baomidou.mybatisplus.core.metadata.IPage<com.koolearn.bms.entity.InboundOrder> byMat =
                inboundOrderService.getOrderPage(1L, 10L, null, null, null, null, null, null, null, "贴片电容");
        assertTrue(byMat.getTotal() >= 1, "按物料名称应能查到入库单");
        com.baomidou.mybatisplus.core.metadata.IPage<com.koolearn.bms.entity.InboundOrder> byDate =
                inboundOrderService.getOrderPage(1L, 10L, null, null, null, null, null,
                        java.time.LocalDate.now().minusDays(1).toString(), java.time.LocalDate.now().toString(), null);
        assertTrue(byDate.getTotal() >= 1, "按入库时间范围应能查到入库单");
    }

    @Test
    void outboundDraftLocksStockConfirmDecreasesAndUnlocks() {
        // 准备物料库存
        Material mat = new Material();
        mat.setMaterialName("电阻");
        mat.setPackageType("0805");
        mat.setStock(new BigDecimal("100"));
        mat.setLockStock(BigDecimal.ZERO);
        materialService.save(mat);

        OutboundOrderDTO dto = new OutboundOrderDTO();
        dto.setOutType(1);
        dto.setApplyUser("eng01");
        OutStorageItemDTO item = new OutStorageItemDTO();
        item.setMaterialId(mat.getId());
        item.setOutNum(new BigDecimal("10"));
        dto.setItemList(Collections.singletonList(item));

        outboundOrderService.saveDraft(dto);
        OutboundOrder order = outboundOrderService.getOne(new LambdaQueryWrapper<OutboundOrder>()
                .eq(OutboundOrder::getApplyUser, "eng01")
                .eq(OutboundOrder::getOrderStatus, 0)
                .orderByDesc(OutboundOrder::getCreateTime)
                .last("limit 1"));
        assertNotNull(order);

        Material afterLock = materialService.getById(mat.getId());
        assertEquals(0, afterLock.getLockStock().compareTo(new BigDecimal("10")), "草稿提交后应占用10");
        assertEquals(0, afterLock.getStock().compareTo(new BigDecimal("100")), "总库存不变");

        // 出库单号符合 J+日期 格式
        OutboundOrderDTO detail = outboundOrderService.getDetailById(order.getId());
        assertNotNull(detail.getOutboundCode());
        assertTrue(detail.getOutboundCode().startsWith("J"), "出库单号应以J开头");

        outboundOrderService.confirmOut(order.getId(), "wh01");

        Material afterOut = materialService.getById(mat.getId());
        assertEquals(0, afterOut.getStock().compareTo(new BigDecimal("90")), "确认出库后库存应减少10");
        assertEquals(0, afterOut.getLockStock().compareTo(BigDecimal.ZERO), "出库后占用应释放");

        Long outCnt = outRecordMapper.selectCount(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<OutRecord>()
                .eq(OutRecord::getMaterialId, mat.getId()));
        assertEquals(1L, outCnt, "应写入一条出库记录");
    }

    @Test
    void outboundRejectReleasesLock() {
        Material mat = new Material();
        mat.setMaterialName("电感");
        mat.setStock(new BigDecimal("50"));
        mat.setLockStock(BigDecimal.ZERO);
        materialService.save(mat);

        OutboundOrderDTO dto = new OutboundOrderDTO();
        dto.setOutType(1);
        dto.setApplyUser("eng01");
        OutStorageItemDTO item = new OutStorageItemDTO();
        item.setMaterialId(mat.getId());
        item.setOutNum(new BigDecimal("5"));
        dto.setItemList(Collections.singletonList(item));
        outboundOrderService.saveDraft(dto);
        OutboundOrder order = outboundOrderService.getOne(new LambdaQueryWrapper<OutboundOrder>()
                .eq(OutboundOrder::getApplyUser, "eng01")
                .eq(OutboundOrder::getOrderStatus, 0)
                .orderByDesc(OutboundOrder::getCreateTime)
                .last("limit 1"));
        assertNotNull(order);

        outboundOrderService.rejectOut(order.getId());

        Material after = materialService.getById(mat.getId());
        assertEquals(0, after.getLockStock().compareTo(BigDecimal.ZERO), "驳回后占用应释放");
        assertEquals(0, after.getStock().compareTo(new BigDecimal("50")), "驳回不影响总库存");
    }

    @Test
    void outboundRejectsInsufficientStock() {
        Material mat = new Material();
        mat.setMaterialName("二极管");
        mat.setStock(new BigDecimal("3"));
        mat.setLockStock(BigDecimal.ZERO);
        materialService.save(mat);

        OutboundOrderDTO dto = new OutboundOrderDTO();
        dto.setOutType(1);
        dto.setApplyUser("eng01");
        OutStorageItemDTO item = new OutStorageItemDTO();
        item.setMaterialId(mat.getId());
        item.setOutNum(new BigDecimal("10"));
        dto.setItemList(Collections.singletonList(item));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> outboundOrderService.saveDraft(dto));
        assertTrue(ex.getMessage().contains("库存不足"));
        Material after = materialService.getById(mat.getId());
        assertEquals(0, after.getLockStock().compareTo(BigDecimal.ZERO), "失败时不应占用库存");
    }
}
