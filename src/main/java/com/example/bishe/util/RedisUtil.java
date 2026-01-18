package com.example.bishe.util;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
public class RedisUtil {
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    //写入缓存
    public boolean set(String key, Object value){
        try {
            redisTemplate.opsForValue().set(key, value);
            return true;
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }

    //写入缓存并设置过期时间
    public boolean set(String key, Object value, long time){
        try {
            if (time > 0){
                redisTemplate.opsForValue().set(key, value, time,TimeUnit.SECONDS);
            }else {
                redisTemplate.opsForValue().set(key, value);
            }
            return true;
        }catch (Exception e){
            return false;
        }
    }

    //获取缓存
    public Object get(String key){
        try{
            return key==null?null:redisTemplate.opsForValue().get(key);
        }catch (Exception e){
            redisTemplate.delete( key);
            return null;
        }

    }
    // 添加一个安全的get方法，指定返回类型
    public <T> T get(String key, Class<T> clazz) {
        try {
            Object value = get(key);
            if (value == null) return null;

            // 如果是目标类型直接返回
            if (clazz.isInstance(value)) {
                return clazz.cast(value);
            }

            // 否则尝试转换
            return objectMapper.convertValue(value, clazz);
        } catch (Exception e) {
            System.err.println("Redis获取指定类型数据失败，键：" + key + ", 错误：" + e.getMessage());
            return null;
        }
    }

    //删除单个缓存
    public boolean del(String key){
        try {
            return Boolean.TRUE.equals(redisTemplate.delete(key));
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    //批量删除匹配的键
    public long delPattern(String pattern){
        try{
            Set<String> keys = redisTemplate.keys(pattern);
            if(keys != null && !keys.isEmpty()){
                return redisTemplate.delete(keys);
            }
            return 0;
        }catch (Exception e){
            e.printStackTrace();
            return 0;
        }
    }
    //判断缓存中是否有对应的value
    public boolean hasKey(String key){
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }


    //设置过期时间
    public boolean expire(String key, long time){
        try {
            return Boolean.TRUE.equals(redisTemplate.expire(key, time, TimeUnit.SECONDS));
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
