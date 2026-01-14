package com.example.bishe.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.bishe.entity.Role;
import com.example.bishe.mapper.RoleMapper;
import com.example.bishe.service.RoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RoleServiceImpl extends ServiceImpl<RoleMapper, Role>
        implements RoleService {

    @Override
    @Cacheable(value = "role", key = "#id")
    public Role getById(Integer id) {
        return super.getById(id);
    }
}