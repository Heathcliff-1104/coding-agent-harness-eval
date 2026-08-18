package com.koolearn.bms.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.koolearn.bms.dto.OutboundOrderDTO;
import com.koolearn.bms.dto.OutStorageItemDTO;
import com.koolearn.bms.entity.Material;
import com.koolearn.bms.entity.OutboundOrder;
import com.koolearn.bms.entity.User;
import com.koolearn.bms.mapper.UserMapper;
import com.koolearn.bms.service.MaterialService;
import com.koolearn.bms.service.OutboundOrderService;
import com.koolearn.bms.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 回调与导出端点测试：
 * 1. 出库驳回回调必须释放锁定库存（此前泄漏锁定）+ 状态置 2
 * 2. /inbound/updateStatus 端点已移除（404）
 * 3. /inRecord/export、/outRecord/export 返回 200 xlsx
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CallbackAndExportTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MaterialService materialService;

    @Autowired
    private OutboundOrderService outboundOrderService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Material createMaterial(BigDecimal stock) {
        Material mat = new Material();
        mat.setMaterialName("回调测试物料");
        mat.setMaterialCode("MTR-CB-" + System.nanoTime());
        mat.setStock(stock);
        mat.setLockStock(BigDecimal.ZERO);
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

    private User createUser(String username, String role) {
        User u = new User();
        u.setUsername(username);
        u.setPassword(passwordEncoder.encode("Abcdefg1!"));
        u.setRealName(username);
        u.setRole(role);
        u.setStatus(1);
        userMapper.insert(u);
        return u;
    }

    private String token(User u) {
        return JwtUtil.generate(u.getId(), u.getUsername(), u.getRole());
    }

    @Test
    void refusedCallbackReleasesLockedStock() throws Exception {
        Material mat = createMaterial(new BigDecimal("100"));
        Long orderId = saveDraftWithNum(mat, new BigDecimal("30"));
        assertEquals(0, new BigDecimal("30").compareTo(materialService.getById(mat.getId()).getLockStock()),
                "草稿应锁定30");

        OutboundOrder order = outboundOrderService.getById(orderId);
        order.setDingInstanceId("inst-refuse-001");
        outboundOrderService.updateById(order);

        mockMvc.perform(post("/outbound/dingtalk/callback")
                        .header("X-Callback-Token", "test-callback-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"instanceId\":\"inst-refuse-001\",\"result\":\"refuse\"}"))
                .andExpect(status().isOk());

        Material after = materialService.getById(mat.getId());
        assertEquals(0, BigDecimal.ZERO.compareTo(after.getLockStock()), "驳回回调应释放锁定库存");
        assertEquals(2, outboundOrderService.getById(orderId).getOrderStatus(), "驳回回调应置状态=2");
    }

    @Test
    void inboundUpdateStatusEndpointRemoved() throws Exception {
        // 端点已移除：即使带合法 token 也应 404（而不是绕过服务状态流转直接改状态）
        User admin = createUser("adm_rem_" + System.nanoTime(), "admin");
        mockMvc.perform(post("/inbound/updateStatus")
                        .param("id", "1")
                        .param("status", "2")
                        .header("Authorization", token(admin)))
                .andExpect(status().isNotFound());
    }

    @Test
    void inRecordExportReturnsXlsx() throws Exception {
        User admin = createUser("adm_irec_" + System.nanoTime(), "admin");
        mockMvc.perform(get("/inRecord/export")
                        .param("startDate", "2026-01-01")
                        .param("endDate", "2026-12-31")
                        .param("keyword", "电阻")
                        .param("billNo", "RK-1")
                        .header("Authorization", token(admin)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")));
    }

    @Test
    void outRecordExportReturnsXlsx() throws Exception {
        User admin = createUser("adm_orec_" + System.nanoTime(), "admin");
        mockMvc.perform(get("/outRecord/export")
                        .param("outboundCode", "OUT")
                        .param("startTime", "2026-01-01 00:00:00")
                        .param("endTime", "2026-12-31 23:59:59")
                        .header("Authorization", token(admin)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")));
    }
}
