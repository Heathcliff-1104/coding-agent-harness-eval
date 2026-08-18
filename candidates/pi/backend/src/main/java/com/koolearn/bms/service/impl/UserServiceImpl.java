package com.koolearn.bms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.koolearn.bms.entity.User;
import com.koolearn.bms.mapper.UserMapper;
import com.koolearn.bms.service.UserService;
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
        if (user.getUsername() == null || user.getUsername().trim().isEmpty()) {
            throw new RuntimeException("用户名不能为空");
        }
        if (user.getRealName() == null || user.getRealName().trim().isEmpty()) {
            throw new RuntimeException("真实姓名不能为空");
        }
        if (user.getPhone() != null && !user.getPhone().isEmpty()
                && !user.getPhone().matches("^1[3-9]\\d{9}$")) {
            throw new RuntimeException("手机号格式不正确");
        }
        LambdaQueryWrapper<User> qw = new LambdaQueryWrapper<>();
        qw.eq(User::getUsername, user.getUsername());
        if (baseMapper.selectCount(qw) > 0) {
            throw new RuntimeException("用户名已存在");
        }
        if (user.getPhone() != null && !user.getPhone().isEmpty()) {
            LambdaQueryWrapper<User> phoneQw = new LambdaQueryWrapper<>();
            phoneQw.eq(User::getPhone, user.getPhone());
            if (baseMapper.selectCount(phoneQw) > 0) {
                throw new RuntimeException("手机号已注册");
            }
        }
        String pwd = user.getPassword();
        String policyMsg = com.koolearn.bms.util.PasswordPolicyUtil.validate(pwd);
        if (policyMsg != null) {
            throw new RuntimeException(policyMsg);
        }
        user.setPassword(passwordEncoder.encode(pwd));
        // 安全修复：注册用户强制分配最低权限工程师角色，忽略请求体中的角色，防止越权
        user.setRole("engineer");
        user.setStatus(1);
        user.setDingtalkUnionId(null);
        try {
            baseMapper.insert(user);
        } catch (org.springframework.dao.DuplicateKeyException e) {
            throw new RuntimeException("用户名或手机号已存在");
        }
    }
}
