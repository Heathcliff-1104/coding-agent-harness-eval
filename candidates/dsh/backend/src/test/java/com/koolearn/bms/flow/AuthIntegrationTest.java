package com.koolearn.bms.flow;

import com.koolearn.bms.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 鉴权集成测试：无token 401、工程师访问管理员接口 403、管理员放行、禁用用户401。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    void noTokenReturns401() throws Exception {
        mockMvc.perform(get("/user/page"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void engineerForbiddenFromAdminApi() throws Exception {
        String token = JwtUtil.generate(3L, "eng01", "engineer");
        mockMvc.perform(get("/user/page").header("Authorization", token))
                .andExpect(status().isForbidden());
    }

    @Test
    void engineerForbiddenFromInboundManagement() throws Exception {
        String token = JwtUtil.generate(3L, "eng01", "engineer");
        mockMvc.perform(get("/inbound/page").header("Authorization", token))
                .andExpect(status().isForbidden());
    }

    @Test
    void engineerCanAccessMaterialSearch() throws Exception {
        String token = JwtUtil.generate(3L, "eng01", "engineer");
        mockMvc.perform(get("/material/page").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void adminCanAccessUserPage() throws Exception {
        String token = JwtUtil.generate(1L, "admin", "admin");
        mockMvc.perform(get("/user/page").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void disabledUserRejectedEvenWithValidToken() throws Exception {
        // 专用测试用户，避免影响其他用例
        jdbcTemplate.update("INSERT INTO sys_user (username, password, real_name, phone, dept, role, status) "
                + "VALUES ('eng_disabled', '$2b$10$QLxeH.e8TByQ9EjFCUDC9OKAYAJ9d4UxwLLAiw3/SmK8ey5VEC3P2', '禁用用户', '13800000999', '硬件部', 'engineer', 0)");
        Long id = jdbcTemplate.queryForObject("SELECT id FROM sys_user WHERE username='eng_disabled'", Long.class);
        String token = JwtUtil.generate(id, "eng_disabled", "engineer");
        mockMvc.perform(get("/material/page").header("Authorization", token))
                .andExpect(status().isUnauthorized());
        jdbcTemplate.update("DELETE FROM sys_user WHERE username='eng_disabled'");
    }
}
