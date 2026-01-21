package com.example.bishe.common.aspect;

import com.example.bishe.common.annotation.RateLimit;
import com.example.bishe.common.context.BaseContext;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.StaticScriptSource;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Aspect
@Component
@Slf4j
public class RateLimitAspect {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    private DefaultRedisScript<Long> limitScript;

    @PostConstruct
    public void init(){
        limitScript =new DefaultRedisScript<>();
        limitScript.setResultType(Long.class);
        //从resources中获取lua文件
        limitScript.setLocation(new ClassPathResource("lua/limit.lua"));
    }

    @Around("@annotation(rateLimit)")
    public Object intercept(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        // 构建Key:限流前缀 + 用户ID
        String key = rateLimit.key() + BaseContext.getCurrentId();
        long now = System.currentTimeMillis();
        long window =rateLimit.time() * 1000L;
        // 执行Lua脚本
        Long result = stringRedisTemplate.execute(
                limitScript,
                Collections.singletonList(key),
                String.valueOf(now),
                String.valueOf(window),
                String.valueOf(rateLimit.count())
        );
        if(result != null && result == 0){
            log.warn("用户{}触发限流", BaseContext.getCurrentId());
            throw new RuntimeException("请求太频繁,请稍后再试");
        }

        return joinPoint.proceed();
    }
}
