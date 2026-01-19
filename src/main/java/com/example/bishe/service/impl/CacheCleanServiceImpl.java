package com.example.bishe.service.impl;

import com.example.bishe.service.CacheCleanService;
import com.example.bishe.util.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class CacheCleanServiceImpl implements CacheCleanService {
    @Autowired
    private RedisUtil redisUtil;


    @Override
    public void cleanUserCache(Long userId) {
        redisUtil.del("userCourseScore:"+userId);
        redisUtil.delPattern("recommend_user_"+userId+"*");
    }

    @Override
    public void cleanCourseGlobalCache() {
        redisUtil.delPattern("hot_courses_*");
        redisUtil.delPattern("course::*");
    }

    @Override
    public void cleanDashboardCache() {
        redisUtil.del("dashboard:course_stats");
    }

    @Async("logExecutor")
    @Override
    public void cleanAllAfterAction(Long userId) {
        cleanUserCache(userId);
        cleanCourseGlobalCache();
        cleanDashboardCache();
    }
}
