//package com.example.bishe.admin;
//
//import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
//
//public class AdminPasswordGenerator {
//    public static void main(String[] args) {
//        String adminPlainPassword = "admin"; // 比如 "Admin@123456"
//        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
//        String adminHashPassword = encoder.encode(adminPlainPassword); // 生成符合系统规则的密文
//        System.out.println("Admin密码密文：" + adminHashPassword);
//        // 输出类似：$2a$10$xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx（和普通用户password字段格式一致）
//    }
//}