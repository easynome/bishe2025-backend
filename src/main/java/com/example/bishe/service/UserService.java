package com.example.bishe.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.bishe.entity.User;
import jakarta.servlet.http.HttpServletRequest;

public interface UserService extends IService<User> {
    void updatePassword(String username, String oldPassword, String newPassword);

}


