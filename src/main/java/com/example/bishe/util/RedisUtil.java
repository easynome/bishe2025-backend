package com.example.bishe.util;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class RedisUtil {
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

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
            e.printStackTrace();
            return null;
        }

    }
    // 添加一个安全的get方法，指定返回类型
    //因为在RedisConfig中配置了Jackson的类型保留(PolymorphicTypeValidator)，所以这里需要一个安全的get方法，指定返回类型
    public <T> T get(String key, Class<T> clazz) {
        try {
            Object value = get(key);
            if (value == null) return null;
            //直接尝试强转，如果类型不匹配，clazz.cast会抛出异常
            return clazz.cast(value);
        } catch (Exception e) {
            log.error("Redis获取数据类型转换失败，key:{},期待类型：{}",key,clazz);
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
    public long delPattern(String pattern) {

        return redisTemplate.execute(connection -> {
            long count = 0;
            //使用迭代器模式，每次扫描100个
            try (Cursor<byte[]> cursor = connection.scan(
                    ScanOptions.scanOptions()
                            .match(pattern)
                            .count(100)
                            .build())) {
                while (cursor.hasNext()) {
                    connection.del(cursor.next());
                    count++;
                }
            } catch (Exception e) {
                    log.error("SCAN模式匹配删除异常",e);
                }
                return count;
            }, true);

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
