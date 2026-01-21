package com.example.bishe.config;

import com.example.bishe.interceptor.JwtInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private JwtInterceptor jwtInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry  registry){
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/**")
                // 1. 业务白名单
                .excludePathPatterns(
                        "/api/login",
                        "/api/register",
                        "/api/course/list",
                        "/api/course/data"
                )
                // 2. 静态资源与错误处理
                .excludePathPatterns(
                        "/error",
                        "/static/**",
                        "/favicon.ico",
                        "/resources/**"
                )
                // 3. Swagger/Knife4j 专用白名单（最关键）
                .excludePathPatterns(
                        "/doc.html",
                        "/webjars/**",
                        "/v3/api-docs",        // 精确匹配
                        "/v3/api-docs/**",     // 匹配分组
                        "/swagger-resources",
                        "/swagger-resources/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html"
                );
    }
}
