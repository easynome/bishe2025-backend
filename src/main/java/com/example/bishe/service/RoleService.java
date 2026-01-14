package com.example.bishe.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.bishe.entity.Role;

import java.util.List;

public interface RoleService extends IService<Role> {
    Role getById(Integer  id);
}