package com.example.bishe.common.constants;

/**
 * Redis业务常量类
 * 统一管理Key前缀与过期时间，便于维护
 */
public class RedisConstants {

    //推荐系统相关
    public static final String RECOMMEND_USER_PREFIX = "recommend:user:";
    public static final String HOT_COURSE_PREFIX = "recommend:hot:";

    //课程缓存相关
    public static final String COURSE_CACHE_PREFIX="course:detail:";
    public static final String COURSE_SCORE_PREFIX="course:score:";

    //统计看板
    public static final String DASHBOARD_STATS_KEY="dashboard:course_stats";
    //容灾降级相关
    public static final String FAILED_LOGS_KEY="audit:failed_logs";

    // 缓存的过期时间
    public static final long RECOMMEND_TTL =900L ; //15分钟
    public static final long HOT_COURSE_TTL = 1800L; //30分钟
}
