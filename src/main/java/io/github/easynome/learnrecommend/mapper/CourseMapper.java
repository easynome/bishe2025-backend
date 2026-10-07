package io.github.easynome.learnrecommend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.easynome.learnrecommend.entity.Course;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CourseMapper extends BaseMapper<Course> {

}
