package com.example.bishe.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.bishe.entity.UserCourseScore;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserCourseScoreMapper extends BaseMapper<UserCourseScore> {
    @Select("SELECT * FROM user_course_score WHERE user_id = #{userId} AND course_id = #{courseId}")
    UserCourseScore findByUserIdAndCourseId(@Param("userId") Long userId,
                                              @Param("courseId") Long courseId);
}