package io.github.easynome.learnrecommend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.easynome.learnrecommend.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
