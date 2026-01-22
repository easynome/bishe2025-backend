package com.example.bishe.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedissonConfig {

    @Value("${spring.data.redis.host}")
    private String host;

    @Value("${spring.data.redis.port}")
    private String port;

    public RedissonClient redissonClient(){
        Config config =new Config();
        //单机模式,如果是集群则用userClusterServers
        config.useSingleServer()
                .setAddress("redis://"+host+":"+port);
        return Redisson.create(config);
    }
}
