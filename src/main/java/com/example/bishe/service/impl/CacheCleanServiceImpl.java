package com.example.bishe.service.impl;

import com.example.bishe.common.constants.RedisConstants;
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
        redisUtil.del(RedisConstants.COURSE_SCORE_PREFIX +userId);
        redisUtil.delPattern(RedisConstants.RECOMMEND_USER_PREFIX+userId+"*");
    }

    @Override
    public void cleanCourseGlobalCache() {
        redisUtil.delPattern(RedisConstants.HOT_COURSE_PREFIX+"*");
        redisUtil.delPattern(RedisConstants.COURSE_CACHE_PREFIX+"*");
    }

    @Override
    public void cleanDashboardCache() {
        redisUtil.del(RedisConstants.DASHBOARD_STATS_KEY);
    }

    @Async("logExecutor")
    @Override
    public void cleanAllAfterAction(Long userId) {
        cleanUserCache(userId);
        cleanCourseGlobalCache();
        cleanDashboardCache();
    }
}
