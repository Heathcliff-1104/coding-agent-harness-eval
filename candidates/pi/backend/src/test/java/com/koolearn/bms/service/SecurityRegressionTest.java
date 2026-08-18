package com.koolearn.bms.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.koolearn.bms.dto.InboundOrderDTO;
import com.koolearn.bms.entity.InStorageItem;
import com.koolearn.bms.entity.InboundOrder;
import com.koolearn.bms.entity.Material;
import com.koolearn.bms.entity.User;
import com.koolearn.bms.mapper.UserMapper;
import com.koolearn.bms.service.InboundOrderService;
import com.koolearn.bms.service.MaterialService;
import com.koolearn.bms.util.CaptchaUtil;
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
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 注册/密码/草稿编辑安全回归测试：
 * 1. 注册请求体携带 id/role/status 被忽略（RegisterDTO 防批量赋值）→ 角色强制 engineer、id 自增、状态启用
 * 2. 两次密码不一致拒绝
 * 3. changePwd 强制密码策略（8-20位、≥3类字符）
 * 4. 入库草稿非待审批状态不允许编辑（editDraft 状态守卫）
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SecurityRegressionTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private InboundOrderService inboundOrderService;

    @Autowired
    private MaterialService materialService;

    /** 生成验证码并反射读取明文，用于走真实注册端点（验证码本身不对外暴露明文） */
    private String[] generateCaptcha() throws Exception {
        CaptchaUtil.CaptchaResult cr = CaptchaUtil.generate();
        java.lang.reflect.Field storeField = CaptchaUtil.class.getDeclaredField("STORE");
        storeField.setAccessible(true);
        ConcurrentHashMap<String, Object> store =
                (ConcurrentHashMap<String, Object>) storeField.get(null);
        Object entry = store.get(cr.key);
        java.lang.reflect.Field codeField = entry.getClass().getDeclaredField("code");
        codeField.setAccessible(true);
        return new String[]{cr.key, (String) codeField.get(entry)};
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
    void registerIgnoresMassAssignedFields() throws Exception {
        String[] cap = generateCaptcha();
        String username = "hacker_" + System.nanoTime();
        mockMvc.perform(post("/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"Abcdefg1!\",\"confirmPassword\":\"Abcdefg1!\","
                                + "\"realName\":\"黑客\",\"phone\":\"13900139000\",\"dept\":\"硬件部\","
                                + "\"captchaKey\":\"" + cap[0] + "\",\"captchaCode\":\"" + cap[1] + "\","
                                + "\"id\":999,\"role\":\"admin\",\"status\":0,\"dingtalkUnionId\":\"fake\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        User saved = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        assertNotNull(saved, "注册用户应保存成功");
        assertEquals("engineer", saved.getRole(), "请求体中的 role=admin 必须被忽略，强制工程师");
        assertEquals(1, saved.getStatus(), "请求体中的 status=0 必须被忽略，默认启用");
        assertNotEquals(999L, saved.getId(), "请求体中的 id 必须被忽略，使用自增主键");
        assertNull(saved.getDingtalkUnionId(), "请求体中的 dingtalkUnionId 必须被忽略");
    }

    @Test
    void registerRejectsMismatchedConfirmPassword() throws Exception {
        String[] cap = generateCaptcha();
        mockMvc.perform(post("/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"mismatch_" + System.nanoTime() + "\",\"password\":\"Abcdefg1!\",\"confirmPassword\":\"Abcdefg2!\","
                                + "\"realName\":\"测试\",\"captchaKey\":\"" + cap[0] + "\",\"captchaCode\":\"" + cap[1] + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    void changePwdEnforcesPasswordPolicy() throws Exception {
        User u = createUser("weakpwd_" + System.nanoTime(), "engineer");
        String tk = token(u);
        // 长度不足8位
        mockMvc.perform(put("/user/changePwd").header("Authorization", tk)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"oldPassword\":\"Abcdefg1!\",\"newPassword\":\"abc123\"}"))
                .andExpect(jsonPath("$.code").value(500));
        // 8位以上但字符类别不足（只有小写+数字=2类）
        mockMvc.perform(put("/user/changePwd").header("Authorization", tk)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"oldPassword\":\"Abcdefg1!\",\"newPassword\":\"abcdefgh123\"}"))
                .andExpect(jsonPath("$.code").value(500));
        // 原密码错误
        mockMvc.perform(put("/user/changePwd").header("Authorization", tk)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"oldPassword\":\"WrongPass1!\",\"newPassword\":\"NewPass123!\"}"))
                .andExpect(jsonPath("$.code").value(500));
        // 合规密码成功
        mockMvc.perform(put("/user/changePwd").header("Authorization", tk)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"oldPassword\":\"Abcdefg1!\",\"newPassword\":\"NewPass123!\"}"))
                .andExpect(jsonPath("$.code").value(200));
        // 密码确实已更新
        User reload = userMapper.selectById(u.getId());
        assertTrue(passwordEncoder.matches("NewPass123!", reload.getPassword()), "新密码应已加密保存");
    }

    @Test
    void materialUpdateIgnoresStockAndLockStockFromBody() throws Exception {
        Material mat = new Material();
        mat.setMaterialName("物料更新安全");
        mat.setStock(new BigDecimal("100"));
        mat.setLockStock(new BigDecimal("10"));
        materialService.save(mat);
        User admin = createUser("adm_mat_" + System.nanoTime(), "admin");

        // 请求体携带伪造的 stock/lockStock/version：必须被忽略（库存只能经出入库流程变更）
        mockMvc.perform(put("/material/update").header("Authorization", token(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":" + mat.getId() + ",\"materialName\":\"改名后\","
                                + "\"stock\":99999,\"lockStock\":88888,\"version\":999}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        Material after = materialService.getById(mat.getId());
        assertEquals("改名后", after.getMaterialName(), "普通字段应更新成功");
        assertEquals(0, new BigDecimal("100").compareTo(after.getStock()), "请求体中的 stock 必须被忽略");
        assertEquals(0, new BigDecimal("10").compareTo(after.getLockStock()), "请求体中的 lockStock 必须被忽略");
    }

    @Test
    void inboundEditDraftRejectedWhenNotDraftStatus() {
        Material mat = new Material();
        mat.setMaterialName("入库编辑测试");
        mat.setMaterialCode("MTR-ED-" + System.nanoTime());
        mat.setStock(BigDecimal.ZERO);
        mat.setLockStock(BigDecimal.ZERO);
        materialService.save(mat);

        InboundOrderDTO dto = new InboundOrderDTO();
        dto.setBillNo("RK-" + System.nanoTime());
        dto.setSupplier("S");
        dto.setUserName("U");
        InStorageItem item = new InStorageItem();
        item.setMaterialId(mat.getId());
        item.setNum(new BigDecimal("10"));
        item.setBatchNo("B1");
        dto.setItemList(Collections.singletonList(item));
        inboundOrderService.saveDraft(dto);

        InboundOrder order = inboundOrderService.list(new LambdaQueryWrapper<InboundOrder>()
                .eq(InboundOrder::getBillNo, dto.getBillNo())).get(0);
        inboundOrderService.confirmIn(order.getId(), "库管员");

        assertThrows(RuntimeException.class, () -> inboundOrderService.editDraft(dto, order.getId()),
                "已入库单据不允许编辑");
    }
}
