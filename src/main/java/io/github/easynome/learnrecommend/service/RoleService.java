package io.github.easynome.learnrecommend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import io.github.easynome.learnrecommend.entity.Role;

import java.util.List;

public interface RoleService extends IService<Role> {
    Role getById(Integer  id);
}