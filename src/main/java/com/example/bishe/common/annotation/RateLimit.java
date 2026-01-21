package com.example.bishe.common.annotation;

import com.example.bishe.common.enums.LimitType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    // 限流key前缀
    String key() default "rate_limit";
    //时间窗口,单位秒
    int time() default 60;
    //在时间窗口内的最大请求次数
    int count() default 10;
    //限流类型,可以根据IP或者UserID 进行限流
    LimitType limitType() default LimitType.USER;

}
