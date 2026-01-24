package com.example.bishe.service.listener;

import com.example.bishe.common.constants.RedisConstants;
import com.example.bishe.config.RabbitMQConfig;
import com.example.bishe.dto.RecommendMsgDTO;
import com.example.bishe.service.CourseService;

import com.example.bishe.service.RecommendService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Slf4j
public class RecommendListener {

    @Autowired
    private RecommendService recommendService;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_NAME)
    public void handleRecommendCalculate(RecommendMsgDTO msg){
        Long userId = msg.getUserId() ;

        log.info("MQ收到计算任务，开始为用户{}预热推荐数据...",userId);

        // 调用分布式锁的计算方法
        // MQ走【查缓存->拿锁->计算->存缓存】
        // recommend拿分布式锁-> 缓存没值
        // ->执行recommendationCalculation，执行完把结果set到缓存Redis->释放锁
        recommendService.recommend(userId,5);

        log.info("MQ结束计算任务，用户{}的推荐数据已预热完成...",userId);

    }
}
