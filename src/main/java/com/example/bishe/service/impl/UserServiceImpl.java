package com.example.bishe.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.bishe.entity.User;
import com.example.bishe.mapper.UserMapper;
import com.example.bishe.service.UserService;
import lombok.RequiredArgsConstructor;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService{


    @Override
    @Cacheable(value="user",key="#username")
    public User getByUsername(String username) {
        return this.getOne(Wrappers.<User>lambdaQuery()
                .eq(User::getUsername, username));
    }

    @Override
    public boolean hasAdminRole(Long userId) {
        User user = this.getById(userId);
        return user != null && user.getRoleId() == 3;
    }

    @Override
    public boolean hasTeacherRole(Long userId) {
        User user = this.getById(userId);
        return user != null && user.getRoleId() == 2;
    }

    @Override
    @CacheEvict(value="user",key="#user.username")
    public boolean updateById(User user) {
        return super.updateById(user);
    }
    @Override
    public void updatePassword(String username, String oldPassword, String newPassword) {
        //TODO: 查用户
        User user =this.lambdaQuery().eq(User::getUsername, username).one();
        if(user == null) throw new RuntimeException("用户不存在");

        //TODO: 验证旧密码
        if (!BCrypt.checkpw(oldPassword, user.getPassword())){
            throw new RuntimeException("旧密码错误");
        }
        String encryptedNewPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt());
        //TODO: 修改密码
        this.lambdaUpdate()
                .eq(User::getUsername, username)
                .set(User::getPassword, encryptedNewPassword)
                .update();
    }
}
