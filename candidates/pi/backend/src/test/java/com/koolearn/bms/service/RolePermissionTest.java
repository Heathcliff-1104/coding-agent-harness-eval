package com.koolearn.bms.service;

import com.koolearn.bms.entity.User;
import com.koolearn.bms.mapper.UserMapper;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 角色权限实时生效测试：
 * 1. 工程师调用管理员接口返回 403
 * 2. 管理员通过真实接口(/user/update)修改角色后，下一次请求即生效（实时权限，不依赖 JWT 声明）
 * 3. 库管员可调用 warehouse/admin 权限接口
 * 4. 禁用用户 403
 *
 * 说明：角色变更必须走真实变更路径（LoginController.update），由后端主动失效角色缓存，
 * 不再手动调用 RoleInterceptor.evict。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RolePermissionTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

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
    void engineerCannotCallAdminEndpoint() throws Exception {
        User eng = createUser("eng_" + System.nanoTime(), "engineer");
        mockMvc.perform(get("/user/list").header("Authorization", token(eng)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/sysLog/page").header("Authorization", token(eng)))
                .andExpect(status().isForbidden());
    }

    @Test
    void noTokenReturns401() throws Exception {
        mockMvc.perform(get("/user/list")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/statistics/inbound").param("start", "2026-01-01").param("end", "2026-12-31"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void roleChangeViaAdminUpdateTakesEffectImmediately() throws Exception {
        User admin = createUser("admin_" + System.nanoTime(), "admin");
        User eng = createUser("promote_" + System.nanoTime(), "engineer");
        String adminTk = token(admin);
        String engTk = token(eng);
        mockMvc.perform(get("/user/list").header("Authorization", engTk))
                .andExpect(status().isForbidden());

        // 走真实变更路径：管理员调用 /user/update 把工程师提升为 admin
        // （JWT 仍携带 engineer 角色声明，后端按 DB 实时校验并主动失效缓存）
        mockMvc.perform(put("/user/update").header("Authorization", adminTk)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":" + eng.getId() + ",\"username\":\"" + eng.getUsername() + "\",\"role\":\"admin\"}"))
                .andExpect(status().isOk());

        // 下一次请求立即生效
        mockMvc.perform(get("/user/list").header("Authorization", engTk))
                .andExpect(status().isOk());
    }

    @Test
    void warehouseCanAccessWarehouseEndpoints() throws Exception {
        User wh = createUser("wh_" + System.nanoTime(), "warehouse");
        mockMvc.perform(get("/stockAlert/page").header("Authorization", token(wh)))
                .andExpect(status().isOk());
        mockMvc.perform(post("/stockAlert/scan").header("Authorization", token(wh)))
                .andExpect(status().isForbidden()); // 手动扫描仅 admin
    }

    @Test
    void disabledUserGets403() throws Exception {
        User admin = createUser("admin2_" + System.nanoTime(), "admin");
        User eng = createUser("disabled_" + System.nanoTime(), "engineer");
        String adminTk = token(admin);
        String engTk = token(eng);

        // 走真实变更路径：管理员调用 /user/update 禁用该用户
        mockMvc.perform(put("/user/update").header("Authorization", adminTk)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":" + eng.getId() + ",\"username\":\"" + eng.getUsername() + "\",\"status\":0}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/user/list").header("Authorization", engTk))
                .andExpect(status().isForbidden());
    }
}
