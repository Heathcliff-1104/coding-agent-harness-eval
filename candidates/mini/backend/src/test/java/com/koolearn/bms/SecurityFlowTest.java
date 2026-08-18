package com.koolearn.bms;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.koolearn.bms.entity.User;
import com.koolearn.bms.service.UserService;
import com.koolearn.bms.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.url=jdbc:h2:mem:bms;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.sql.init.mode=always",
        "bms.jwt.secret=test-jwt-secret-0123456789-0123456789-0123456789"
})
@AutoConfigureMockMvc
class SecurityFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    private User createUser(String username, String role) {
        User u = new User();
        u.setUsername(username);
        u.setPassword(passwordEncoder.encode("Passw0rd!"));
        u.setRealName(username);
        u.setPhone("138" + (10000000 + (int)(Math.random() * 89999999)));
        u.setDept("IT");
        u.setRole(role);
        u.setStatus(1);
        userService.save(u);
        return u;
    }

    @Test
    void roleChangeTakesEffectImmediatelyWithoutRelogin() throws Exception {
        User engineer = createUser("perm_engineer", "engineer");
        String token = jwtUtil.generate(engineer.getId(), engineer.getUsername(), "engineer");

        // 初始角色 engineer
        mockMvc.perform(get("/user/info").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("engineer"));

        // 管理员将其角色改为 warehouse
        engineer.setRole("warehouse");
        userService.updateById(engineer);

        // 同一 token 再次请求，应立即生效
        mockMvc.perform(get("/user/info").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("warehouse"));
    }

    @Test
    void disabledUserTokenIsRejected() throws Exception {
        User engineer = createUser("perm_disabled", "engineer");
        String token = jwtUtil.generate(engineer.getId(), engineer.getUsername(), "engineer");

        mockMvc.perform(get("/user/info").header("Authorization", token))
                .andExpect(status().isOk());

        engineer.setStatus(0);
        userService.updateById(engineer);

        mockMvc.perform(get("/user/info").header("Authorization", token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void noTokenIsRejected() throws Exception {
        mockMvc.perform(get("/user/info"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void registeredUsersCannotSetAdminRole() throws Exception {
        // 通过 service 注册，即使 DTO 无 role 字段也确保 engineer
        User admin = createUser("perm_admin", "admin");
        assertThat(admin.getRole()).isEqualTo("admin");
        User existing = userService.getOne(new LambdaQueryWrapper<User>().eq(User::getUsername, "perm_admin"));
        assertThat(existing.getRole()).isEqualTo("admin");
    }
}
