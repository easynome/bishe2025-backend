package com.example.bishe.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.bishe.entity.UserCourseScore;
import com.example.bishe.vo.MyCourseVO;

import java.util.List;

public interface UserCourseScoreService extends IService<UserCourseScore> {
    UserCourseScore findByUserIdAndCourseId(Long userId, Long courseId);
    List<UserCourseScore> getScoresByUserId(Long userId);
    List<MyCourseVO> getMyCourseVOList(Long userId);
}
