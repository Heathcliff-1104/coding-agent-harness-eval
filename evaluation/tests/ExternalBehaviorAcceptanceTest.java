package com.koolearn.bms.acceptance;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.koolearn.bms.util.CaptchaUtil;
import com.koolearn.bms.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * Evaluator-owned, candidate-independent HTTP acceptance tests.
 *
 * This exact source file is injected unchanged into every candidate after the
 * candidate artifacts have been frozen. It intentionally tests observable
 * behaviour instead of candidate-specific service implementations.
 */
@SpringBootTest(properties = {
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.url=jdbc:h2:mem:evaluator_hidden;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.sql.init.mode=always",
        "bms.jwt.secret=evaluator-hidden-jwt-secret-0123456789-0123456789-0123456789",
        "sys.role.cache.ttl.ms=600000"
})
@ActiveProfiles("test")
@AutoConfigureMockMvc
class ExternalBehaviorAcceptanceTest {

    private static final String BCRYPT_PASSWORD =
            "$2b$10$QLxeH.e8TByQ9EjFCUDC9OKAYAJ9d4UxwLLAiw3/SmK8ey5VEC3P2";

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper mapper;
    @Autowired private ApplicationContext context;

    private String suffix;

    @BeforeEach
    void uniqueSuffix() {
        suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    }

    @Test
    void protectedEndpointRejectsMissingAndMalformedTokens() throws Exception {
        assertEquals(401, mockMvc.perform(get("/user/page"))
                .andReturn().getResponse().getStatus());
        assertEquals(401, mockMvc.perform(get("/user/page").header("Authorization", "not-a-jwt"))
                .andReturn().getResponse().getStatus());
    }

    @Test
    void signedAdminClaimCannotOverrideDatabaseEngineerRole() throws Exception {
        long id = insertUser("claim_" + suffix, "engineer", 1);
        String token = token(id, "claim_" + suffix, "admin");

        int status = mockMvc.perform(get("/user/page").header("Authorization", token))
                .andReturn().getResponse().getStatus();

        assertEquals(403, status,
                "A signed JWT admin claim must not override the user's current database role");
    }

    @Test
    void disabledAndMissingUsersCannotUseOtherwiseValidTokens() throws Exception {
        long disabledId = insertUser("disabled_" + suffix, "admin", 0);
        String disabledToken = token(disabledId, "disabled_" + suffix, "admin");
        int disabledStatus = mockMvc.perform(get("/user/page").header("Authorization", disabledToken))
                .andReturn().getResponse().getStatus();
        assertTrue(disabledStatus == 401 || disabledStatus == 403,
                "Disabled account must be rejected, got HTTP " + disabledStatus);

        String missingToken = token(9_000_000_000L, "missing_" + suffix, "admin");
        int missingStatus = mockMvc.perform(get("/user/page").header("Authorization", missingToken))
                .andReturn().getResponse().getStatus();
        assertTrue(missingStatus == 401 || missingStatus == 403,
                "Non-existent account must be rejected, got HTTP " + missingStatus);
    }

    @Test
    void publicRegistrationRequiresCaptcha() throws Exception {
        String username = "nocaptcha_" + suffix;
        String phone = phoneFor(suffix, 1);
        String body = registrationJson(username, phone, "Valid1!x", "Valid1!x",
                null, null, "engineer", 1);

        MvcResult result = mockMvc.perform(post("/user/register")
                        .contentType("application/json").content(body))
                .andReturn();

        assertNotEquals(200, apiCode(result), "Registration without a captcha must fail");
        assertEquals(0, countUser(username), "Failed registration must not persist a user");
    }

    @Test
    void publicRegistrationRequiresMatchingPasswordConfirmation() throws Exception {
        String username = "mismatch_" + suffix;
        String phone = phoneFor(suffix, 2);
        CaptchaAnswer captcha = captchaAnswer();
        String body = registrationJson(username, phone, "Valid1!x", "Different2@x",
                captcha.key, captcha.code, "engineer", 1);

        MvcResult result = mockMvc.perform(post("/user/register")
                        .contentType("application/json").content(body))
                .andReturn();

        assertNotEquals(200, apiCode(result), "Mismatched password confirmation must fail");
        assertEquals(0, countUser(username), "Failed registration must not persist a user");
    }

    @Test
    void publicRegistrationCannotMassAssignPrivilege() throws Exception {
        String username = "massassign_" + suffix;
        String phone = phoneFor(suffix, 3);
        CaptchaAnswer captcha = captchaAnswer();
        String body = registrationJson(username, phone, "Valid1!x", "Valid1!x",
                captcha.key, captcha.code, "admin", 0);

        MvcResult result = mockMvc.perform(post("/user/register")
                        .contentType("application/json").content(body))
                .andReturn();

        assertEquals(200, apiCode(result), "A valid registration should succeed");
        Map<String, Object> row = jdbc.queryForMap(
                "SELECT role, status, password FROM sys_user WHERE username=?", username);
        assertEquals("engineer", String.valueOf(row.get("role")),
                "Public registration must force the engineer role");
        assertEquals(1, ((Number) row.get("status")).intValue(),
                "Public registration must force enabled status");
        String storedPassword = String.valueOf(row.get("password"));
        assertNotEquals("Valid1!x", storedPassword, "Password must not be stored in plaintext");
        assertTrue(storedPassword.startsWith("$2"), "Password must be BCrypt encoded");
    }

    @Test
    void roleChangeTakesEffectOnTheExistingToken() throws Exception {
        String adminName = "admin_" + suffix;
        long adminId = insertUser(adminName, "admin", 1);
        String adminToken = token(adminId, adminName, "admin");

        String targetName = "promote_" + suffix;
        long targetId = insertUser(targetName, "engineer", 1);
        String existingTargetToken = token(targetId, targetName, "engineer");

        int before = mockMvc.perform(get("/user/page").header("Authorization", existingTargetToken))
                .andReturn().getResponse().getStatus();
        assertEquals(403, before, "Engineer must initially be denied the admin endpoint");

        MvcResult update = mockMvc.perform(put("/user/update")
                        .header("Authorization", adminToken)
                        .contentType("application/json")
                        .content("{\"id\":" + targetId + ",\"role\":\"admin\",\"status\":1}"))
                .andReturn();
        assertEquals(200, apiCode(update), "Admin role update should succeed");

        int after = mockMvc.perform(get("/user/page").header("Authorization", existingTargetToken))
                .andReturn().getResponse().getStatus();
        assertEquals(200, after,
                "The same existing token must reflect the database role change immediately");
    }

    @Test
    void adminUserListingDoesNotLeakPasswordHashes() throws Exception {
        String username = "listing_" + suffix;
        long id = insertUser(username, "admin", 1);
        String token = token(id, username, "admin");

        MvcResult result = mockMvc.perform(get("/user/page")
                        .param("keyword", username)
                        .header("Authorization", token))
                .andReturn();

        assertEquals(200, result.getResponse().getStatus());
        assertEquals(200, apiCode(result));
        String response = result.getResponse().getContentAsString();
        assertFalse(response.contains(BCRYPT_PASSWORD), "User API must not expose password hashes");
        assertFalse(response.contains("\"password\":\""),
                "User API must not expose a non-null password field");
    }

    private long insertUser(String username, String role, int status) {
        jdbc.update("INSERT INTO sys_user (username,password,real_name,phone,dept,role,status) " +
                        "VALUES (?,?,?,?,?,?,?)",
                username, BCRYPT_PASSWORD, username, phoneFor(username, 9), "验收部", role, status);
        return jdbc.queryForObject("SELECT id FROM sys_user WHERE username=?", Long.class, username);
    }

    private int countUser(String username) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM sys_user WHERE username=?", Integer.class, username);
    }

    private int apiCode(MvcResult result) throws Exception {
        JsonNode json = mapper.readTree(result.getResponse().getContentAsString());
        return json.path("code").asInt(-1);
    }

    private String token(long userId, String username, String role) throws Exception {
        Method generate = JwtUtil.class.getMethod("generate", Long.class, String.class, String.class);
        Object target = null;
        if (!Modifier.isStatic(generate.getModifiers())) {
            Map<String, JwtUtil> beans = context.getBeansOfType(JwtUtil.class);
            assertFalse(beans.isEmpty(), "Non-static JwtUtil.generate requires a Spring bean");
            target = beans.values().iterator().next();
        }
        return (String) generate.invoke(target, userId, username, role);
    }

    @SuppressWarnings("unchecked")
    private CaptchaAnswer captchaAnswer() throws Exception {
        CaptchaUtil.CaptchaResult generated = CaptchaUtil.generate();
        Field storeField = CaptchaUtil.class.getDeclaredField("STORE");
        storeField.setAccessible(true);
        Map<String, Object> store = (Map<String, Object>) storeField.get(null);
        Object entry = store.get(generated.key);
        assertNotNull(entry, "Generated captcha must be present in the one-time store");
        Field codeField = entry.getClass().getDeclaredField("code");
        codeField.setAccessible(true);
        return new CaptchaAnswer(generated.key, String.valueOf(codeField.get(entry)));
    }

    private String registrationJson(String username, String phone, String password,
                                    String confirmPassword, String captchaKey, String captchaCode,
                                    String role, int status) throws Exception {
        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("username", username);
        body.put("password", password);
        body.put("confirmPassword", confirmPassword);
        body.put("realName", "外部验收用户");
        body.put("phone", phone);
        body.put("dept", "验收部");
        body.put("role", role);
        body.put("status", status);
        if (captchaKey != null) body.put("captchaKey", captchaKey);
        if (captchaCode != null) body.put("captchaCode", captchaCode);
        return mapper.writeValueAsString(body);
    }

    private String phoneFor(String value, int salt) {
        long hash = Integer.toUnsignedLong((value + salt).hashCode());
        return "1" + (3 + (hash % 7)) + String.format("%09d", hash % 1_000_000_000L);
    }

    private static final class CaptchaAnswer {
        final String key;
        final String code;

        CaptchaAnswer(String key, String code) {
            this.key = key;
            this.code = code;
        }
    }
}
