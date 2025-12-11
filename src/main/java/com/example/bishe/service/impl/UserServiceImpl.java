package com.example.bishe.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.bishe.entity.User;
import com.example.bishe.mapper.UserMapper;
import com.example.bishe.service.UserService;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService{

    @Override
    public void updatePassword(String username, String oldPassword, String newPassword) {
        //TODO: 查用户
        User user =this.lambdaQuery().eq(User::getUsername, username).one();
        if(user == null) throw new RuntimeException("用户不存在");

        //TODO: 验证旧密码
        if (!user.getPassword().equals(oldPassword)){
            throw new RuntimeException("旧密码错误");
        }
        //TODO: 修改密码
        this.lambdaUpdate()
                .eq(User::getUsername, username)
                .set(User::getPassword, newPassword)
                .update();

    }
}
