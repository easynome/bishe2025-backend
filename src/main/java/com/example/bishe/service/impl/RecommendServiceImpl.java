package com.example.bishe.service.impl;

import com.example.bishe.entity.Course;
import com.example.bishe.entity.UserCourseScore;
import com.example.bishe.mapper.CourseMapper;
import com.example.bishe.mapper.UserCourseScoreMapper;
import com.example.bishe.service.RecommendService;
import com.example.bishe.util.CosineUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 推荐服务实现类，基于用户协同过滤算法为用户推荐课程。
 */
@Service
@RequiredArgsConstructor
public class RecommendServiceImpl implements RecommendService {
    private final UserCourseScoreMapper scoreMapper;
    private final CourseMapper courseMapper;

    /**
     * 根据用户ID为其推荐最相关的课程列表（使用基于用户的协同过滤）。
     *
     * @param userId 用户ID，用于查找该用户的行为数据并进行相似度计算
     * @param topN   需要返回的推荐课程数量上限
     * @return 推荐的课程列表，按相关性从高到低排序，最多包含topN个元素
     */
    @Override
    public List<Course> recommend(Long userId, int topN) {
        // 获取所有用户对课程的评分记录
        List<UserCourseScore> all = scoreMapper.selectList(null);

        long userScoreCount=all.stream()
                .filter(s->s.getUserId()!=null&&s.getUserId().equals(userId))
                .count();

        if(userScoreCount<3){
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
        return weightSum.entrySet().stream()
                .map(en -> Map.entry(en.getKey(), en.getValue() / simSum.get(en.getKey()))) // 计算预测评分
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())             // 按评分降序排列
                .limit(topN)                                                               // 截取前topN项
                .map(en -> courseMapper.selectById(en.getKey()))                           // 查询对应课程实体
                .collect(Collectors.toList());                                             // 转换为List返回
    }

    /**
     * 冷启动兜底方法: 获取最热门的课程
     * @param allScores
     * @param topN
     * @return
     */
    @Override
    public List<Course> getHotCourse(List<UserCourseScore> allScores, int topN){
        return allScores.stream()
                .collect(Collectors.groupingBy(UserCourseScore::getCourseId,
                        Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
                .limit(topN)
                .map(entry -> courseMapper.selectById(entry.getKey()))//根据ID查询课程实体
                .collect(Collectors.toList());
    }
}
