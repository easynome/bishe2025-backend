package com.example.bishe.util;
import java.util.*;


/**
 * 基于用户的余弦相似度 + Top-N 推荐
 */
public class CosineUtil {

    /** 计算两个用户的余弦相似度 */
    public static double cosine(Map<Long, Integer> a, Map<Long, Integer> b) {
        double dot = 0, normA = 0, normB = 0;
        for (Long k : a.keySet()) {
            int va = a.get(k);
            int vb = b.getOrDefault(k, 0);
            dot += va * vb;
            normA += va * va;
            normB += vb * vb;
        }
        return normA == 0 || normB == 0 ? 0 : dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    /** 为指定用户生成 Top-N 推荐课程 ID */
    public static List<Long> recommend(Map<Long, Map<Long, Integer>> all,
                                       long targetUserId,
                                       int n) {
        Map<Long, Integer> target = all.get(targetUserId);
        if (target == null) return Collections.emptyList();

        // 1. 计算与其他用户的相似度
        List<Map.Entry<Long, Double>> simList = new ArrayList<>();
        for (Long user : all.keySet()) {
            if (user.equals(targetUserId)) continue;
            double sim = cosine(target, all.get(user));
            if (sim > 0) simList.add(new AbstractMap.SimpleEntry<>(user, sim));
        }
        simList.sort((e1, e2) -> -Double.compare(e1.getValue(), e2.getValue()));

        // 2. 聚合相似用户的高分课程
        Map<Long, Double> weightSum = new HashMap<>();
        Map<Long, Double> simSum = new HashMap<>();
        for (Map.Entry<Long, Double> e : simList) {
            Long user = e.getKey();
            double sim = e.getValue();
            for (Map.Entry<Long, Integer> scoreE : all.get(user).entrySet()) {
                Long course = scoreE.getKey();
                if (target.containsKey(course)) continue; // 已学跳过
                int score = scoreE.getValue();
                weightSum.merge(course, sim * score, Double::sum);
                simSum.merge(course, sim, Double::sum);
            }
        }

        // 3. 按加权平均分排序取 Top-N
        List<Map.Entry<Long, Double>> rank = new ArrayList<>();
        for (Long course : weightSum.keySet()) {
            rank.add(new AbstractMap.SimpleEntry<>(course, weightSum.get(course) / simSum.get(course)));
        }
        rank.sort((e1, e2) -> -Double.compare(e1.getValue(), e2.getValue()));
        List<Long> result = new ArrayList<>();
        for (int i = 0; i < Math.min(n, rank.size()); i++) {
            result.add(rank.get(i).getKey());
        }
        return result;
    }

    /* ------ 单元测试 ------ */
    public static void main(String[] args) {
        // 模拟 DB 数据：user -> {course:score}
        Map<Long, Map<Long, Integer>> all = new HashMap<>();
        all.put(1L, Map.of(1L, 5, 2L, 4, 3L, 4));
        all.put(2L, Map.of(1L, 4, 2L, 5, 4L, 5));
        all.put(3L, Map.of(2L, 3, 3L, 5, 4L, 4));
        all.put(4L, Map.of(1L, 2, 4L, 5));

        List<Long> top2 = recommend(all, 1L, 2);
        System.out.println("推荐课程 = " + top2);   // 期望：[4, 2]
    }
}