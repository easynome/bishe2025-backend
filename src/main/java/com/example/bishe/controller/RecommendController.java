package com.example.bishe.controller;

import com.example.bishe.common.annotation.RateLimit;
import com.example.bishe.common.enums.LimitType;
import com.example.bishe.entity.Course;
import com.example.bishe.common.result.R;
import com.example.bishe.service.RecommendService;
import com.example.bishe.util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 推荐控制器类
 * 处理课程推荐相关的HTTP请求
 */
@Tag(name="课程推荐接口",description = "课程推荐接口")
@RestController
@RequestMapping("/api/recommend")
@RequiredArgsConstructor
public class RecommendController {
    private final RecommendService recommendService;
    /**
     *
     * 获取用户课程推荐列表
     * @param topN 推荐课程数量，默认为5
     * @param request 请求对象，用于获取用户ID
     * @return 响应结果，包含推荐课程列表
     */

    @RateLimit(count = 200, time = 60, limitType = LimitType.USER)
    @Operation(summary = "获取用户课程推荐列表",description = "基于用户协同过滤算法实时计算（或从Redis获取）的个性化推荐")
    @GetMapping
    public R<List<Course>> recommend(@RequestParam(defaultValue = "5") int topN,
                                     HttpServletRequest request) {
        try {
            // 从token获取当前用户ID
            String token = request.getHeader("Authorization");
            if (token == null || !token.startsWith("Bearer ")) {
                return R.failed("未登录");
            }
            token = token.substring(7);
            Long userId = JwtUtil.getUserIdFromToken(token);

            if (userId == null) {
                return R.failed("用户ID获取失败");
            }
            // 调用推荐服务
            List<Course> recommendations = recommendService.recommend(userId, topN);
            return R.success(recommendations);
        } catch (Exception e) {
            e.printStackTrace();
            return R.failed("获取推荐失败: " + e.getMessage());
        }
    }

}
