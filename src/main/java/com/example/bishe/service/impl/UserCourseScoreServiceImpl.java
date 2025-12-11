package com.example.bishe.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.bishe.entity.UserCourseScore;
import com.example.bishe.mapper.UserCourseScoreMapper;
import com.example.bishe.service.UserCourseScoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserCourseScoreServiceImpl
        extends ServiceImpl<UserCourseScoreMapper, UserCourseScore>
        implements UserCourseScoreService {

    private final UserCourseScoreMapper userCourseScoreMapper;


    @Override
    public UserCourseScore findByUserIdAndCourseId(Long userId, Long courseId) {
        return userCourseScoreMapper.findByUserIdAndCourseId(userId, courseId);
    }
}
