package com.koolearn.bms.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CaptchaUtilTest {

    @Test
    void generateReturnsImage() {
        CaptchaUtil.CaptchaResult cr = CaptchaUtil.generate();
        assertNotNull(cr.key);
        assertTrue(cr.image.startsWith("data:image/png;base64,"));
    }

    @Test
    void verifyConsumesCode() {
        CaptchaUtil.CaptchaResult cr = CaptchaUtil.generate();
        assertFalse(CaptchaUtil.verify(cr.key, "XXXX"));
        assertFalse(CaptchaUtil.verify(cr.key, "XXXX"));
    }

    @Test
    void verifyNullReturnsFalse() {
        assertFalse(CaptchaUtil.verify(null, null));
        assertFalse(CaptchaUtil.verify("key", null));
        assertFalse(CaptchaUtil.verify(null, "code"));
    }
}
