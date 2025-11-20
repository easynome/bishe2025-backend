package com.example.bishe.controller;

import com.example.bishe.entity.Course;
import com.example.bishe.entity.R;
import com.example.bishe.service.RecommenService;
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
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class RecommendController {
    private final RecommenService recommenService;

    /**
     * 获取用户课程推荐列表
     * 根据用户ID和推荐数量返回个性化的课程推荐
     *
     * @param userId 用户ID，用于识别需要推荐的用户
     * @param topN 推荐课程数量，默认为5
     * @return 包含推荐课程列表的响应结果
     */
    @GetMapping("/recommend")
    public R<List<Course>> recommend(@RequestParam Long userId,
                                     @RequestParam(defaultValue = "5") int topN) {
        return R.success(recommenService.recommend(userId, topN));
    }
}
