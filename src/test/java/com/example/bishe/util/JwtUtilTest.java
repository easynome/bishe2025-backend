package com.example.bishe.util;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * JwtUtil 单元测试：令牌生成、解析与非法令牌处理，不依赖 Spring 容器。
 */
class JwtUtilTest {

    @Test
    @DisplayName("生成的令牌可解析出用户名")
    void testSubjectRoundTrip() {
        String token = JwtUtil.generate("alice", 42L);
        Claims claims = JwtUtil.parse(token);
        assertEquals("alice", claims.getSubject());
    }

    @Test
    @DisplayName("userId 声明可从令牌还原")
    void testUserIdRoundTrip() {
        String token = JwtUtil.generate("bob", 10086L);
        assertEquals(10086L, JwtUtil.getUserIdFromToken(token));
    }

    @Test
    @DisplayName("非法令牌解析时抛出 RuntimeException")
    void testInvalidToken() {
        assertThrows(RuntimeException.class, () -> JwtUtil.parse("not.a.valid.token"));
    }

    @Test
    @DisplayName("非法令牌获取 userId 时抛出 RuntimeException")
    void testInvalidTokenGetUserId() {
        assertThrows(RuntimeException.class,
                () -> JwtUtil.getUserIdFromToken("garbage-token"));
    }
}
