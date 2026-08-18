package com.koolearn.bms;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.koolearn.bms.entity.User;
import com.koolearn.bms.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;

import java.nio.charset.StandardCharsets;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.url=jdbc:h2:mem:bms;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.sql.init.mode=always"
})
@AutoConfigureMockMvc
class RegistrationRoleRegressionTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @Test
    void publicRegistrationCannotInjectAdminRole() throws Exception {
        String username = "attacker_admin";
        String body = "{"
                + "\"username\":\"" + username + "\","
                + "\"password\":\"Passw0rd!\","
                + "\"realName\":\"Attacker\","
                + "\"phone\":\"13800138000\","
                + "\"dept\":\"IT\","
                + "\"role\":\"admin\""
                + "}";

        mockMvc.perform(post("/user/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isOk());

        User persisted = userService.getOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        assertThat(persisted).isNotNull();
        assertThat(persisted.getRole()).isEqualTo("engineer");
        assertThat(persisted.getRole()).isNotEqualTo("admin");
        // password-hashing preserved: persisted password must be a BCrypt hash
        assertThat(persisted.getPassword()).startsWith("$2");
    }

    @Test
    void duplicateUsernameRegistrationStillRejected() throws Exception {
        String username = "dup_user";
        String body = "{"
                + "\"username\":\"" + username + "\","
                + "\"password\":\"Passw0rd!\","
                + "\"realName\":\"Dup\","
                + "\"phone\":\"13900139000\","
                + "\"dept\":\"IT\""
                + "}";

        mockMvc.perform(post("/user/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isOk());

        String respBody = mockMvc.perform(post("/user/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(respBody).contains("用户名已存在");
    }
}
