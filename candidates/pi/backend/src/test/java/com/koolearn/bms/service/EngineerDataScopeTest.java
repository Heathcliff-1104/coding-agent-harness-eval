package com.koolearn.bms.service;

import com.koolearn.bms.dto.OutboundOrderDTO;
import com.koolearn.bms.dto.OutStorageItemDTO;
import com.koolearn.bms.entity.Material;
import com.koolearn.bms.entity.User;
import com.koolearn.bms.mapper.UserMapper;
import com.koolearn.bms.service.MaterialService;
import com.koolearn.bms.service.OutboundOrderService;
import com.koolearn.bms.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collections;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 工程师数据范围测试：
 * 非 admin/warehouse 用户（工程师）查询出库单只能看到自己的申请单（applyUser == 当前登录人）；
 * admin 可看到全部。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class EngineerDataScopeTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private OutboundOrderService outboundOrderService;

    @Autowired
    private MaterialService materialService;

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

    private void createOutboundOrder(Material mat, String applyUser, BigDecimal num) {
        OutboundOrderDTO dto = new OutboundOrderDTO();
        dto.setOutType(1);
        dto.setApplyUser(applyUser);
        OutStorageItemDTO item = new OutStorageItemDTO();
        item.setMaterialId(mat.getId());
        item.setMaterialCode(mat.getMaterialCode());
        item.setBatchNo("B1");
        item.setOutNum(num);
        dto.setItemList(Collections.singletonList(item));
        outboundOrderService.saveDraft(dto);
    }

    @Test
    void engineerSeesOnlyOwnOutboundOrders() throws Exception {
        User eng = createUser("eng_ds_" + System.nanoTime(), "engineer");
        User admin = createUser("adm_ds_" + System.nanoTime(), "admin");

        Material mat = new Material();
        mat.setMaterialName("数据范围物料");
        mat.setMaterialCode("MTR-DS-" + System.nanoTime());
        mat.setStock(new BigDecimal("500"));
        mat.setLockStock(BigDecimal.ZERO);
        materialService.save(mat);

        createOutboundOrder(mat, eng.getUsername(), new BigDecimal("10")); // 工程师自己的
        createOutboundOrder(mat, "someone-else", new BigDecimal("10"));    // 别人的

        // 工程师：只能看到自己的 1 张单，且 applyUser 都是自己
        mockMvc.perform(get("/outbound/page").header("Authorization", token(eng)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].applyUser").value(eng.getUsername()));

        // 管理员：能看到全部 2 张单
        mockMvc.perform(get("/outbound/page").header("Authorization", token(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(2));
    }

    @Test
    void engineerCannotSeeOthersOrderDetail() throws Exception {
        User eng = createUser("eng_det_" + System.nanoTime(), "engineer");

        Material mat = new Material();
        mat.setMaterialName("数据范围明细物料");
        mat.setMaterialCode("MTR-DD-" + System.nanoTime());
        mat.setStock(new BigDecimal("100"));
        mat.setLockStock(BigDecimal.ZERO);
        materialService.save(mat);

        createOutboundOrder(mat, "someone-else", new BigDecimal("10"));

        // 拿到别人的单号
        Long otherId = outboundOrderService.list(null).stream()
                .filter(o -> "someone-else".equals(o.getApplyUser()))
                .findFirst().get().getId();

        mockMvc.perform(get("/outbound/get/" + otherId).header("Authorization", token(eng)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403));
    }
}
