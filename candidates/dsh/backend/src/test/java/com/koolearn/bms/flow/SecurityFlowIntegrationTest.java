package com.koolearn.bms.flow;

import com.koolearn.bms.dto.OutboundOrderDTO;
import com.koolearn.bms.dto.OutStorageItemDTO;
import com.koolearn.bms.entity.Material;
import com.koolearn.bms.entity.OutboundOrder;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.koolearn.bms.service.MaterialService;
import com.koolearn.bms.service.OutboundOrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 安全流程回归：DingTalk 回调驳回必须释放占用库存（C3）。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityFlowIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private OutboundOrderService outboundOrderService;
    @Autowired private MaterialService materialService;

    @Test
    void dingtalkCallbackRefuseReleasesLockedStock() throws Exception {
        Material mat = new Material();
        mat.setMaterialName("安全测试物料");
        mat.setStock(new BigDecimal("50"));
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
        // 提交审批（演示模式生成模拟 instanceId）
        outboundOrderService.saveOrder(new OutboundOrderDTO() {{
            setId(order.getId());
        }});
        OutboundOrder refreshed = outboundOrderService.getById(order.getId());
        assertNotNull(refreshed.getDingInstanceId(), "审批实例ID应存在");

        // 回调 refuse → 走 rejectOut → 释放占用
        mockMvc.perform(post("/outbound/dingtalk/callback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"instanceId\":\"" + refreshed.getDingInstanceId() + "\",\"result\":\"refuse\"}"))
                .andExpect(status().isOk());

        Material after = materialService.getById(mat.getId());
        assertEquals(0, after.getLockStock().compareTo(BigDecimal.ZERO), "驳回回调后占用应释放");
        assertEquals(0, after.getStock().compareTo(new BigDecimal("50")), "驳回不影响总库存");
        OutboundOrder finalOrder = outboundOrderService.getById(order.getId());
        assertEquals(2, finalOrder.getOrderStatus(), "单据应处于已驳回状态");
    }
}
