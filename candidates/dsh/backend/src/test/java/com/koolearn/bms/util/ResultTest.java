package com.koolearn.bms.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ResultTest {

    @Test
    void successCarriesData() {
        Result<String> r = Result.success("hello");
        assertEquals(200, r.getCode());
        assertEquals("hello", r.getData());
        assertNull(Result.success().getData());
    }

    @Test
    void failCarriesMessageAndCode() {
        Result<String> r = Result.fail("出错了");
        assertEquals(500, r.getCode());
        assertEquals("出错了", r.getMsg());
        Result<String> r2 = Result.fail(429, "太频繁");
        assertEquals(429, r2.getCode());
        assertEquals("太频繁", r2.getMsg());
    }
}
