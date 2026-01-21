package com.example.bishe.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.bishe.entity.UserCourseScore;
import com.example.bishe.vo.MyCourseVO;

import java.util.List;

public interface UserCourseScoreService extends IService<UserCourseScore> {
    // 根据用户id和课程id查询
    UserCourseScore findByUserIdAndCourseId(Long userId, Long courseId);
    // 根据用户id查询
    List<UserCourseScore> getScoresByUserId(Long userId);
    // 获取我的课程列表
    List<MyCourseVO> getMyCourseVOList(Long userId);
}
