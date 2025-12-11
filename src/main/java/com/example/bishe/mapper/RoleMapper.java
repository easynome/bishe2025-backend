package com.example.bishe.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.bishe.entity.Role;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RoleMapper extends BaseMapper<Role> {
    // 不需要额外方法，BaseMapper已经提供基本CRUD
}
