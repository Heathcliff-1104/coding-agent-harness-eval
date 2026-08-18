package com.koolearn.bms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.koolearn.bms.entity.User;
import com.koolearn.bms.mapper.UserMapper;
import com.koolearn.bms.service.UserService;
import com.koolearn.bms.util.PasswordPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public User login(String username, String password) {
        LambdaQueryWrapper<User> qw = new LambdaQueryWrapper<>();
        qw.eq(User::getUsername, username).eq(User::getStatus, 1);
        User user = baseMapper.selectOne(qw);
        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("用户名或密码错误");
        }
        user.setPassword(null);
        return user;
    }

    @Override
    public void register(User user) {
        String username = user.getUsername();
        if (username == null || username.trim().isEmpty()) {
            throw new RuntimeException("用户名不能为空");
        }
        username = username.trim();
        user.setUsername(username);
        if (user.getRealName() == null || user.getRealName().trim().isEmpty()) {
            throw new RuntimeException("真实姓名不能为空");
        }
        if (user.getPhone() == null || user.getPhone().trim().isEmpty()) {
            throw new RuntimeException("手机号不能为空");
        }
        if (!user.getPhone().matches("^1[3-9]\\d{9}$")) {
            throw new RuntimeException("手机号格式不正确");
        }
        LambdaQueryWrapper<User> qw = new LambdaQueryWrapper<>();
        qw.eq(User::getUsername, username);
        if (baseMapper.selectCount(qw) > 0) {
            throw new RuntimeException("用户名已存在");
        }
        LambdaQueryWrapper<User> phoneQw = new LambdaQueryWrapper<>();
        phoneQw.eq(User::getPhone, user.getPhone());
        if (baseMapper.selectCount(phoneQw) > 0) {
            throw new RuntimeException("手机号已注册");
        }
        String pwd = user.getPassword();
        String err = PasswordPolicy.validate(pwd);
        if (err != null) {
            throw new RuntimeException(err);
        }
        user.setPassword(passwordEncoder.encode(pwd));
        // 新用户自动分配工程师权限（最低权限），忽略客户端传入的角色，防止越权注册
        user.setRole("engineer");
        user.setStatus(1);
        baseMapper.insert(user);
    }

    @Override
    public void registerByAdmin(User user) {
        String username = user.getUsername();
        if (username == null || username.trim().isEmpty()) {
            throw new RuntimeException("用户名不能为空");
        }
        username = username.trim();
        user.setUsername(username);
        if (user.getRealName() == null || user.getRealName().trim().isEmpty()) {
            throw new RuntimeException("真实姓名不能为空");
        }
        LambdaQueryWrapper<User> qw = new LambdaQueryWrapper<>();
        qw.eq(User::getUsername, username);
        if (baseMapper.selectCount(qw) > 0) {
            throw new RuntimeException("用户名已存在");
        }
        if (user.getPhone() != null && !user.getPhone().isEmpty()) {
            if (!user.getPhone().matches("^1[3-9]\\d{9}$")) {
                throw new RuntimeException("手机号格式不正确");
            }
            LambdaQueryWrapper<User> phoneQw = new LambdaQueryWrapper<>();
            phoneQw.eq(User::getPhone, user.getPhone());
            if (baseMapper.selectCount(phoneQw) > 0) {
                throw new RuntimeException("手机号已注册");
            }
        }
        String pwd = user.getPassword();
        String err = PasswordPolicy.validate(pwd);
        if (err != null) {
            throw new RuntimeException(err);
        }
        user.setPassword(passwordEncoder.encode(pwd));
        if (user.getRole() == null || user.getRole().trim().isEmpty()) {
            user.setRole("engineer");
        }
        if (user.getStatus() == null) {
            user.setStatus(1);
        }
        baseMapper.insert(user);
    }
}
