package com.koolearn.bms.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordPolicyTest {

    @Test
    void rejectsShortPassword() {
        assertNotNull(PasswordPolicy.validate("Ab1@"));
        assertNotNull(PasswordPolicy.validate(null));
        assertNotNull(PasswordPolicy.validate(""));
    }

    @Test
    void rejectsTooLongPassword() {
        assertNotNull(PasswordPolicy.validate("Ab1@x".repeat(5))); // 25 chars
    }

    @Test
    void rejectsPasswordWithTooFewClasses() {
        assertNotNull(PasswordPolicy.validate("abcdefgh"));   // 只有小写
        assertNotNull(PasswordPolicy.validate("ABCDEFGH"));   // 只有大写
        assertNotNull(PasswordPolicy.validate("12345678"));   // 只有数字
        assertNotNull(PasswordPolicy.validate("abcdefg1"));   // 两类
    }

    @Test
    void acceptsThreeOrFourClasses() {
        assertNull(PasswordPolicy.validate("Abcdef12"));       // 大写+小写+数字
        assertNull(PasswordPolicy.validate("Abcdef@1"));       // 四类
        assertNull(PasswordPolicy.validate("abc@1234"));       // 小写+数字+特殊
    }
}
