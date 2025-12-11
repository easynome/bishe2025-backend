package com.example.bishe.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import io.jsonwebtoken.Claims;
public class JwtUtil {
    //暂时使用硬编码
    private static final String SECRET = "bishe2025bishe2025bishe2025bishe2025bishe202532byte";
    private static final long EXPIRATION = 1;//1天
    private static final SecretKey SECRET_KEY = Keys.hmacShaKeyFor(SECRET.getBytes());

    //生成token
    public static String generate(String username,Long userId){
        Instant now = Instant.now();
        return Jwts.builder()
                .setSubject(username)
                .claim("userId", userId)//添加userId到claims
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plus(EXPIRATION, ChronoUnit.DAYS)))
                .signWith(SECRET_KEY)
                .compact();
    }

    //解析token
    //Claims类型的返回值，包含用户名、用户ID、签发时间、过期时间等信息
    public static Claims parse(String token) {
        try {
//            return Jwts.parser()
//                    .setSigningKey(SECRET_KEY)
//                    .parseClaimsJws(token)
//                    .getBody()
//                    .getSubject();
            return Jwts.parser()
                    .setSigningKey(SECRET_KEY)
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            throw new RuntimeException("Token解析失败", e);
        }
    }

    //从token中获取userId
    public static Long getUserIdFromToken(String token) {
        Claims claims = parse(token);
        return claims.get("userId", Long.class);
    }
//
//    public static Long getUserIdFromRequest(HttpServletRequest request) {
//        String token = request.getHeader("Authorization");
//        if (token != null && token.startsWith("Bearer ")) {
//            token = token.substring(7);
//            return getUserIdFromToken(token);
//        }
//        return null;
//    }

}
