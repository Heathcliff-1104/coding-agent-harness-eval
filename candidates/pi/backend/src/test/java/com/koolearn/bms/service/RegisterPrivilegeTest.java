package com.koolearn.bms.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.koolearn.bms.entity.User;
import com.koolearn.bms.mapper.UserMapper;
import com.koolearn.bms.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 注册权限安全测试：
 * 1. 注册时请求体携带 role=admin 会被强制覆盖为 engineer（防提权）
 * 2. 重复用户名拒绝
 * 3. 空用户名/姓名拒绝
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RegisterPrivilegeTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User buildUser(String username) {
        User u = new User();
        u.setUsername(username);
        u.setPassword("Abcdefg1!");
        u.setRealName("测试用户");
        u.setPhone("13800138000");
        u.setDept("硬件部");
        return u;
    }

    @Test
    void registerWithAdminRoleInBodyIsForcedToEngineer() {
        User u = buildUser("hacker_" + System.nanoTime());
        u.setRole("admin"); // 恶意提权尝试
        userService.register(u);

        User saved = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, u.getUsername()));
        assertNotNull(saved);
        assertEquals("engineer", saved.getRole(), "注册用户必须被强制分配工程师角色");
        assertEquals(1, saved.getStatus());
        assertNotEquals("Abcdefg1!", saved.getPassword(), "密码必须加密存储");
    }

    @Test
    void duplicateUsernameRejected() {
        User u1 = buildUser("dup_" + System.nanoTime());
        userService.register(u1);
        User u2 = buildUser(u1.getUsername());
        assertThrows(RuntimeException.class, () -> userService.register(u2), "重复用户名应被拒绝");
    }

    @Test
    void blankUsernameRejected() {
        User u = buildUser("  ");
        assertThrows(RuntimeException.class, () -> userService.register(u));
    }

    @Test
    void blankRealNameRejected() {
        User u = buildUser("noname_" + System.nanoTime());
        u.setRealName("");
        assertThrows(RuntimeException.class, () -> userService.register(u));
    }

    @Test
    void invalidPhoneRejected() {
        User u = buildUser("badphone_" + System.nanoTime());
        u.setPhone("12345");
        assertThrows(RuntimeException.class, () -> userService.register(u));
    }

    @Test
    void duplicatePhoneRejected() {
        User u1 = buildUser("ph1_" + System.nanoTime());
        userService.register(u1);
        User u2 = buildUser("ph2_" + System.nanoTime());
        u2.setPhone(u1.getPhone());
        assertThrows(RuntimeException.class, () -> userService.register(u2));
    }
}
