package com.example.bishe.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.bishe.entity.Course;
import com.example.bishe.entity.UserCourseScore;
import com.example.bishe.mapper.UserCourseScoreMapper;
import com.example.bishe.service.UserCourseScoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserCourseScoreServiceImpl
        extends ServiceImpl<UserCourseScoreMapper, UserCourseScore>
        implements UserCourseScoreService {

    private final UserCourseScoreMapper userCourseScoreMapper;
    private final CourseServiceImpl courseService;

    @Override
    public UserCourseScore findByUserIdAndCourseId(Long userId, Long courseId) {
        return userCourseScoreMapper.findByUserIdAndCourseId(userId, courseId);
    }

    public void saveScore(UserCourseScore score) {
        // 1. 获取课程当前状态
        Course course = courseService.getById(score.getCourseId());
        if (course != null) {
            // 2. 必须手动设置冗余字段的值
            score.setCourseStatus(course.getStatus());
        }
        this.save(score);
    }
    @Override
    @Cacheable(value="userCourseScore",key="#userId")
    public List<UserCourseScore> getScoresByUserId(Long userId) {
        return this.list(Wrappers.<UserCourseScore>lambdaQuery()
                .eq(UserCourseScore::getUserId,userId)
                .orderByDesc(UserCourseScore::getCreatedAt));
    }
}
