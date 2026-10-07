package io.github.easynome.learnrecommend.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CosineUtil 单元测试：余弦相似度与 Top-N 推荐，不依赖 Spring 容器与外部服务。
 */
class CosineUtilTest {

    private Map<Long, Integer> vec(int... kv) {
        Map<Long, Integer> m = new HashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            m.put((long) kv[i], kv[i + 1]);
        }
        return m;
    }

    @Test
    @DisplayName("相同向量的余弦相似度为 1")
    void testIdentical() {
        Map<Long, Integer> a = vec(1, 3, 2, 4);
        assertEquals(1.0, CosineUtil.cosine(a, a), 1e-9);
    }

    @Test
    @DisplayName("无共同评分项时相似度为 0")
    void testOrthogonal() {
        Map<Long, Integer> a = vec(1, 5);
        Map<Long, Integer> b = vec(2, 5);
        assertEquals(0.0, CosineUtil.cosine(a, b), 1e-9);
    }

    @Test
    @DisplayName("部分重叠时相似度等于已知值 1/√2")
    void testPartialOverlap() {
        Map<Long, Integer> a = vec(1, 1, 2, 1);
        Map<Long, Integer> b = vec(1, 1);
        assertEquals(1 / Math.sqrt(2), CosineUtil.cosine(a, b), 1e-9);
    }

    @Test
    @DisplayName("推荐结果只包含目标用户未评分的课程")
    void testRecommendExcludesRated() {
        Map<Long, Map<Long, Integer>> all = new HashMap<>();
        all.put(1L, vec(1, 5, 2, 4, 3, 4));
        all.put(2L, vec(1, 4, 2, 5, 4, 5));
        all.put(3L, vec(2, 3, 3, 5, 4, 4));
        all.put(4L, vec(1, 2, 4, 5));

        List<Long> result = CosineUtil.recommend(all, 1L, 2);
        // 课程 1/2/3 目标用户已评分，应被过滤；课程 4 是唯一候选且排第一
        assertEquals(1, result.size());
        assertEquals(4L, result.get(0));
    }

    @Test
    @DisplayName("Top-N 参数生效")
    void testTopNLimit() {
        Map<Long, Map<Long, Integer>> all = new HashMap<>();
        all.put(1L, vec(1, 5, 2, 4, 3, 4));
        all.put(2L, vec(1, 4, 2, 5, 4, 5));
        all.put(3L, vec(2, 3, 3, 5, 4, 4));
        all.put(4L, vec(1, 2, 4, 5));

        List<Long> result = CosineUtil.recommend(all, 1L, 1);
        assertTrue(result.size() <= 1);
        assertEquals(4L, result.get(0));
    }

    @Test
    @DisplayName("目标用户不存在时返回空列表")
    void testUnknownUser() {
        Map<Long, Map<Long, Integer>> all = new HashMap<>();
        all.put(2L, vec(1, 5));
        assertTrue(CosineUtil.recommend(all, 99L, 5).isEmpty());
    }
}
