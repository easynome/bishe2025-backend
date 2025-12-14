package com.example.bishe.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// 创建配置类 MybatisPlusConfig.java
@Configuration
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // 添加分页插件
        PaginationInnerInterceptor paginationInterceptor = new PaginationInnerInterceptor();
        paginationInterceptor.setDbType(DbType.MYSQL);  // 数据库类型
        paginationInterceptor.setOverflow(true);  // 超过页数是否回到第一页
        paginationInterceptor.setMaxLimit(500L);  // 单页最大记录数

        interceptor.addInnerInterceptor(paginationInterceptor);
        return interceptor;
    }
}