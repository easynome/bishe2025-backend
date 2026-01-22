package com.example.bishe.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.bishe.common.constants.RedisConstants;
import com.example.bishe.entity.Course;
import com.example.bishe.entity.UserCourseScore;
import com.example.bishe.mapper.CourseMapper;
import com.example.bishe.mapper.UserCourseScoreMapper;
import com.example.bishe.service.RecommendService;
import com.example.bishe.util.CosineUtil;
import com.example.bishe.util.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.management.Query;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 推荐服务实现类，基于用户协同过滤算法为用户推荐课程。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendServiceImpl implements RecommendService {
    private final UserCourseScoreMapper scoreMapper;
    private final CourseMapper courseMapper;
    private final RedisUtil redisUtil;
    // Redisson作用是获取锁
    @Autowired
    private RedissonClient redissonClient;

    //本地锁，用于控制单机开发（如果是分布式环境则使用Redisson）
//    private final Object lock = new Object();

    /**
     * 根据用户ID为其推荐最相关的课程列表（使用基于用户的协同过滤）。
     *
     * @param userId 用户ID，用于查找该用户的行为数据并进行相似度计算
     * @param topN   需要返回的推荐课程数量上限
     * @return 推荐的课程列表，按相关性从高到低排序，最多包含topN个元素
     */
    @Override
    public List<Course> recommend(Long userId, int topN) {
        String lockKey ="lock:recommend:"+userId;
        RLock lock =redissonClient.getLock(lockKey);

        String cacheKey= RedisConstants.RECOMMEND_USER_PREFIX+userId+"_"+topN;

        // 第一次检查缓存
        List< Course> cachedData=redisUtil.get(cacheKey,List.class);
        if(cachedData!=null)return cachedData;

//        QueryWrapper<UserCourseScore> wrapper =new QueryWrapper<UserCourseScore>().last("limit 10000");
        // 缓存不存在，加锁
       try{

           if(lock.tryLock(5,10, TimeUnit.SECONDS)) {
               // 第二次检查缓存（防止在等锁期间缓存被其他线程写入）
               cachedData = redisUtil.get(cacheKey, List.class);
               if (cachedData != null) return cachedData;

               //在锁的保护下执行计算逻辑
               return recommendationCalculation(userId, topN, cacheKey);
           }else{
               log.warn("获取推荐锁超时，使用缓存数据或返回空结果");
               return Collections.emptyList();
           }
        }catch(InterruptedException e){
           Thread.currentThread().interrupt();
           log.error("推荐计算被中断", e);
           return Collections.emptyList();
       }finally {
           // 释放锁
           if(lock.isHeldByCurrentThread()){
               lock.unlock();
           }
       }
    }

    /**
     * 冷启动兜底方法: 获取最热门的课程
     * @param allScores
     * @param topN
     * @return
     */
    @Override
    public List<Course> getHotCourse(List<UserCourseScore> allScores, int topN){
        //1.定义Key
        String cacheKey=RedisConstants.HOT_COURSE_PREFIX+topN;

        //2.尝试从Redis获取数据
        try {
            List<Course> cachedData = redisUtil.get(cacheKey, List.class);
            if (cachedData != null) {
                //如果存在，则直接强转并返回
                return cachedData;
            }
        }catch (Exception e){
            log.error("热门课程缓存读取异常，进入实时计算模式：{}",e.getMessage());
        }

        log.info("开始实时计算热门课程列表");

        //3.如果不存在，则进行数据计算
        List<Long> hotCourseIds =allScores.stream()
                .collect(Collectors.groupingBy(UserCourseScore::getCourseId,Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
                .limit(topN)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        if(hotCourseIds.isEmpty()) return new ArrayList<>();

        List<Course> hotCourses = courseMapper.selectBatchIds(hotCourseIds).stream()
                .filter(course -> course.getStatus() != null && course.getStatus() == 1)
                .collect(Collectors.toList());

        //4.性能优化：用selectBatchIds代替selectById(减少数据库连接次数)
//        List<Course> hotCourses=courseMapper.selectBatchIds(hotCourseIds);
        //保证查询出来的顺序和ID排序一致
        hotCourses.sort(Comparator.comparingInt(c->hotCourseIds.indexOf(c.getId())));

        //5.将结果缓存到Redis中，设置30分钟的过期时间
        try {
            int expireTime = 1800 + new Random().nextInt(600);
            redisUtil.set(cacheKey, hotCourses, expireTime);
        }catch (Exception e){
            log.error("热门课程缓存写入异常：{}",e.getMessage());
        }
        return hotCourses;
    }

    /**
     * 基于用户的协同过滤算法进行推荐计算。
     *
     * @param userId       用户ID
     * @param topN         推荐课程数量上限
     * @param cacheKey     缓存Key
     * @return 推荐的课程列表
     */
    private List<Course> recommendationCalculation(Long userId, int topN,String cacheKey) {
        //开始计算
        // 获取所有用户对课程的评分记录
        List<UserCourseScore> all = scoreMapper.selectList(null);

        long userScoreCount=all.stream()
                .filter(s->s.getUserId()!=null&&s.getUserId().equals(userId))
                .count();

        if(userScoreCount<3){
            System.out.println("用户评分不足3条，走热门兜底");
            return getHotCourse(all,topN);
        }

        // 构建以用户ID为键、课程ID与评分映射为值的数据结构
        Map<Long, Map<Long, Integer>> userMap = all.stream()
                .collect(Collectors.groupingBy(UserCourseScore::getUserId,
                        Collectors.toMap(UserCourseScore::getCourseId,
                                UserCourseScore::getScore)));

        // 获取目标用户的评分向量
        Map<Long, Integer> target = userMap.get(userId);
        if (target == null) return Collections.emptyList();

        // 初始化两个用于加权平均计算的辅助Map：
        // weightSum：存储每个未评分课程的相似度与评分乘积之和
        // simSum：存储每个未评分课程的相似度总和
        Map<Long, Double> weightSum = new HashMap<>();
        Map<Long, Double> simSum = new HashMap<>();

        // 对其他用户逐一计算与目标用户的余弦相似度，并更新推荐分数
        userMap.entrySet().stream()
                .filter(e -> !e.getKey().equals(userId)) // 排除自身
                .forEach(e -> {
                    double sim = CosineUtil.cosine(target, e.getValue()); // 计算余弦相似度
                    if (sim <= 0) return; // 忽略无正相关性的用户

                    // 遍历当前对比用户所评过的课程
                    e.getValue().forEach((courseId, score) -> {
                        // 只处理目标用户尚未评分的课程
                        if (!target.containsKey(courseId)) {
                            weightSum.merge(courseId, sim * score, Double::sum); // 加权评分累计
                            simSum.merge(courseId, sim, Double::sum);           // 相似度累计
                        }
                    });
                });

        // 将加权评分总和除以相似度总和得到最终预测评分，并选出前topN项对应的课程信息
//        List<Course> recommendList =weightSum.entrySet().stream()
//                .map(en -> Map.entry(en.getKey(), en.getValue() / simSum.get(en.getKey())))
//                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
//                .limit(topN)
//                .map(en -> courseMapper.selectById(en.getKey()))
//                .collect(Collectors.toList());

        //优化版，先拿ID列表
        List<Long> hotIds=weightSum.entrySet().stream()
                // 获取课程ID和预测评分
                .map(en -> Map.entry(en.getKey(), en.getValue() / simSum.get(en.getKey())))
                // 按预测评分降序排列
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                // 截取前topN项
                .limit(topN)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());


        List<Course> recommendList=hotIds.isEmpty()?
                new ArrayList<>():
                courseMapper.selectBatchIds(hotIds);
        // 恢复按 hotIds 顺序排序，保证推荐的高分课程在前
        recommendList.sort(Comparator.comparingInt(c -> hotIds.indexOf(c.getId())));

        System.out.println("推荐算法计算完成，准备存入缓存，结果数量：" + recommendList.size());

        if (recommendList.isEmpty()) {
            // 缓存空列表
            redisUtil.set(cacheKey, new ArrayList<>(), 60); // 缓存空列表5分钟
        } else {
            int expireTime=900+new Random().nextInt(600);
            redisUtil.set(cacheKey, recommendList, expireTime);
        }
        return recommendList;
    }
}
