package com.example.bishe.util;// JwtUtilTest.java

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class JwtUtilTest {

    @Test
    void testGetUserIdFromToken() {
        // 假设有一个有效的token
        String token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...";
        Long userId = JwtUtil.getUserIdFromToken(token);
        System.out.println("从token获取的userId: " + userId);
        // 应该输出正确的用户ID
    }
//    @Test
//    void testGetUserIdFromToken() {
//        // 准备测试数据
//        String username = "testuser";
//        Long userId = 123L;
//
//        // 生成有效的token
//        String token = JwtUtil.generate(username, userId);
//
//        // 执行测试
//        Long extractedUserId = JwtUtil.getUserIdFromToken(token);
//
//        // 验证结果
//        assertNotNull(extractedUserId);
//        assertEquals(userId, extractedUserId);
//        System.out.println("从token获取的userId: " + extractedUserId);
//    }

    @Test
    void testGetUsernameFromToken() {
        // 准备测试数据
        String username = "testuser";
        Long userId = 123L;

        // 生成有效的token
        String token = JwtUtil.generate(username, userId);

        // 执行测试
        String extractedUsername = JwtUtil.parse(token).getSubject();

        // 验证结果
        assertNotNull(extractedUsername);
        assertEquals(username, extractedUsername);
        System.out.println("从token获取的username: " + extractedUsername);
    }
}
