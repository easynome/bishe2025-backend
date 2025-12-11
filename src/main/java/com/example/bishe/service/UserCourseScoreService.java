package com.example.bishe.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.bishe.entity.UserCourseScore;

public interface UserCourseScoreService extends IService<UserCourseScore> {
    UserCourseScore findByUserIdAndCourseId(Long userId, Long courseId);
}
