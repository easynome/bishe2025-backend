package io.github.easynome.learnrecommend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import io.github.easynome.learnrecommend.entity.User;


public interface UserService extends IService<User> {
    void updatePassword(String username, String oldPassword, String newPassword);

    User getByUsername(String username);
    boolean hasAdminRole(Long userId);
    boolean hasTeacherRole(Long userId);

}


