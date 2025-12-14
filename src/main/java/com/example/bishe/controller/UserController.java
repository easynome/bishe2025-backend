package com.example.bishe.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.bishe.dto.LoginDTO;
import com.example.bishe.dto.RegisterDTO;
import com.example.bishe.dto.UpdatePwdDTO;
import com.example.bishe.entity.R;
import com.example.bishe.entity.Role;
import com.example.bishe.entity.User;
import com.example.bishe.entity.UserCourseScore;
import com.example.bishe.mapper.UserCourseScoreMapper;
import com.example.bishe.service.RoleService;
import com.example.bishe.service.UserService;
import com.example.bishe.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final RoleService roleService;

    //管理员分页查用户
    @GetMapping("/user/list")
    public R<IPage<User>> list(@RequestParam(defaultValue = "1") long current,
                               @RequestParam(defaultValue = "10") long size){
        return R.success(userService.page(new Page<>(current, size)));
    }

    //用户登录
    @PostMapping("/login")
    public R<Map<String,String>> login(@RequestBody LoginDTO dto){
        try {
            User user = userService.getOne(
                    Wrappers.<User>lambdaQuery().eq(User::getUsername, dto.getUsername()));
            if(user == null){
                return R.failed("用户名或密码错误");
            }
            //
            if(!BCrypt.checkpw(dto.getPassword(), user.getPassword())){
                return R.failed("用户名或密码错误");
            }
            String token =JwtUtil.generate(user.getUsername(),user.getId());
            return R.success(Map.of("token", token));
        }catch (Exception e){
            e.printStackTrace();
            return R.failed("登录失败,请稍后再试");
        }
    }
    //获取用户信息

    @GetMapping("/user/info")
    public R<Map<String, Object>> getUserInfo(@RequestHeader("Authorization") String token) {
        try {
            String cleanToken = token.startsWith("Bearer ") ? token.substring(7) : token;
            String username = JwtUtil.parse(cleanToken).getSubject();

            User user = userService.getOne(
                    Wrappers.<User>lambdaQuery().eq(User::getUsername, username));

            if (user == null) {
                return R.failed("用户不存在");
            }

            // 查询角色名称
            Role role = roleService.getById(user.getRoleId());
            String roleName = role != null ? role.getName() : "未知角色";

            // 返回Map，包含用户信息和角色名称
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("id", user.getId());
            userInfo.put("username", user.getUsername());
            userInfo.put("roleId", user.getRoleId());
            userInfo.put("roleName", roleName);
            userInfo.put("createdAt", user.getCreatedAt());

            return R.success(userInfo);
        } catch (Exception e) {
            e.printStackTrace();
            return R.failed("获取用户信息失败");
        }
    }
    //注册
    @PostMapping("/register")
    public R<String> register(@RequestBody @Valid RegisterDTO dto){
        try{

            if(!dto.getPassword().equals(dto.getConfirmPassword())){
                return R.failed("密码不一致");
            }

            Long count = userService.count(
                    Wrappers.<User>lambdaQuery().eq(User::getUsername, dto.getUsername()));
            if(count > 0) {
                return R.failed("用户名已存在");
            }

            User user = new User();
            user.setUsername(dto.getUsername());

            String encryptedPassword = BCrypt.hashpw(dto.getPassword(), BCrypt.gensalt());
            user.setPassword(encryptedPassword);

            user.setRoleId(1);

            userService.save(user);
            return R.success("注册成功");
        }catch (Exception e){
            e.printStackTrace();
            return R.failed("注册失败"+e.getMessage());
        }

    }
    //登出
    @PostMapping("/user/logout")
    public R<String> logout(){
        return R.success("退出成功");
    }

    //修改密码
    @PostMapping("/user/update_password")
    public R<String> updatePassword(@RequestBody UpdatePwdDTO dto,
                                    @RequestHeader("Authorization") String token){

        String username= JwtUtil.parse(cleanToken( token)).getSubject();
        userService.updatePassword(username,dto.getOldPassword(),dto.getNewPassword());
        return R.success("修改密码成功");
    }
    //清除token
    private String cleanToken(String token) {
        return token.startsWith("Bearer ") ? token.substring(7) : token;
    }

    //获取用户列表
    @GetMapping("/admin/users/list")
    public R<Map<String, Object>> getUsersForAdmin(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer roleId
    ){
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();

        if(keyword != null&& !keyword.trim().isEmpty()){
            wrapper.like(User::getUsername, keyword);
        }

        if(roleId != null){
            wrapper.eq(User::getRoleId, roleId);
        }

//        wrapper.ne(User::getId, currentUserId);
        wrapper.orderByDesc(User::getCreatedAt);

        Page<User> pageInfo = new Page<>(page, size);
        Page<User> userPage = userService.page(pageInfo, wrapper);

        List< User> users = userPage.getRecords();
        Map<Integer, String> roleMap = roleService.list()
                .stream()
                .collect(Collectors.toMap(Role::getId, Role::getName));

        List<Map<String, Object>> userList = users.stream()
                .map(user -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", user.getId());
                    map.put("username", user.getUsername());
                    map.put("roleId", user.getRoleId());
                    map.put("roleName", roleMap.get(user.getRoleId()));
                    map.put("createdAt", user.getCreatedAt());
                    map.put("status", user.getStatus()!= null? user.getStatus() : 1);
                    return map;
                }).collect(Collectors.toList());
        Map<String, Object> result = new HashMap<>();
        result.put("users", userList);
        result.put("total", userPage.getTotal());
        result.put("pages", userPage.getPages());
        result.put("current", userPage.getCurrent());
        result.put("size", userPage.getSize());
        return R.success(result);
    }

    //修改用户角色
    @PostMapping("/admin/users/{id}/role")
    public R<String> updateUserRole(@PathVariable Long id,
                                    @RequestBody Integer roleId,
                                    HttpServletRequest  request) {

        //验证权限（仅管理员可修改用户角色）
        //验证角色ID有效性（1-学生，2-教师，3-管理员）

        User user = userService.getById(id);
        if(user == null) {
            return R.failed("用户不存在");
        }
        Role role = roleService.getById(roleId);
        if(role == null){
            return R.failed("角色不存在");
        }
        user.setRoleId(roleId);
        userService.updateById(user);

        return R.success("更新用户角色成功");
    }

    //修改用户状态
    @PostMapping("/admin/users/{id}/status")
    public R<String> updateUserStatus(@PathVariable Long id,
                                      @RequestBody Integer status,
                                      HttpServletRequest  request) {

        if(status != 1 && status != 0){
            return R.failed("状态值无效");
        }
        User user = userService.getById(id);
        if(user == null){
            return R.failed("用户不存在");
        }
        user.setStatus(status);
        userService.updateById(user);

        return R.success(status == 1 ? "用户已启用" :"用户已禁用");
    }
    /**
     * 校验密码（假设使用BCrypt）
     */
    private boolean checkPassword(String rawPassword, String encodedPassword) {
        // 示例：return BCrypt.checkpw(rawPassword, encodedPassword);
        // 实际项目需替换为真实加密工具
        return rawPassword.equals(encodedPassword); // 占位符，实际不应如此实现！
    }

    /**
     * 生成JWT Token
     */
    private String generateToken(String username,Long userId) {
        return JwtUtil.generate(username,userId);
    }
}

