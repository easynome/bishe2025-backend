package io.github.easynome.learnrecommend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import io.github.easynome.learnrecommend.entity.UserCourseScore;
import io.github.easynome.learnrecommend.vo.MyCourseVO;

import java.util.List;

public interface UserCourseScoreService extends IService<UserCourseScore> {
    // 根据用户id和课程id查询
    UserCourseScore findByUserIdAndCourseId(Long userId, Long courseId);
    // 根据用户id查询
    List<UserCourseScore> getScoresByUserId(Long userId);
    // 获取我的课程列表
    List<MyCourseVO> getMyCourseVOList(Long userId);
}
