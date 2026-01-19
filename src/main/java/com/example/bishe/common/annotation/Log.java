package com.example.bishe.common.annotation;

import java.lang.annotation.*;

//指定注解使用的程序元素类型
@Target({ElementType.PACKAGE, ElementType.METHOD})
//定义注解的保留策略
@Retention(RetentionPolicy.RUNTIME)
//标记注解应被包含在JavaDoc中
@Documented
public @interface Log {
    //模块名称
    public String title() default "";
    /**
     * 功能描述
     * 0其他 1新增 2修改 3删除
     */
    public int businessType() default 0;
}
