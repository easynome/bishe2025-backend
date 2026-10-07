package io.github.easynome.learnrecommend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.easynome.learnrecommend.entity.UserCourseScore;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserCourseScoreMapper extends BaseMapper<UserCourseScore> {
    @Select("SELECT * FROM user_course_score WHERE user_id = #{userId} AND course_id = #{courseId}")
    UserCourseScore findByUserIdAndCourseId(@Param("userId") Long userId,
                                              @Param("courseId") Long courseId);
}