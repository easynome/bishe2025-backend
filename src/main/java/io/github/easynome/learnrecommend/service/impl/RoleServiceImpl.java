package io.github.easynome.learnrecommend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.easynome.learnrecommend.entity.Role;
import io.github.easynome.learnrecommend.mapper.RoleMapper;
import io.github.easynome.learnrecommend.service.RoleService;
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