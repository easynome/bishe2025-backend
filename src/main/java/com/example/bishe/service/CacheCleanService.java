package com.example.bishe.service;

public interface CacheCleanService {

    //清理与特定用户相关的缓存（如个人推荐、个人评分列表）
    void cleanUserCache(Long userId);

    //清理全局课程相关缓存（如热门榜单、课程详情）
    void cleanCourseGlobalCache();

    //清理仪表盘统计缓存
    void cleanDashboardCache();

    //组合清理：当发生评分或状态改变时调用
    void cleanAllAfterAction(Long userId);
}
