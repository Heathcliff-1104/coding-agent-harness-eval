package com.koolearn.bms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.koolearn.bms.dto.UserRegisterDTO;
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
    public void register(UserRegisterDTO dto) {
        LambdaQueryWrapper<User> qw = new LambdaQueryWrapper<>();
        qw.eq(User::getUsername, dto.getUsername());
        if (baseMapper.selectCount(qw) > 0) {
            throw new RuntimeException("用户名已存在");
        }
        if (dto.getPhone() != null && !dto.getPhone().isEmpty()) {
            LambdaQueryWrapper<User> phoneQw = new LambdaQueryWrapper<>();
            phoneQw.eq(User::getPhone, dto.getPhone());
            if (baseMapper.selectCount(phoneQw) > 0) {
                throw new RuntimeException("手机号已注册");
            }
        }
        String pwd = dto.getPassword();
        if (pwd == null || pwd.length() < 8 || pwd.length() > 20) {
            throw new RuntimeException("密码长度须为8~20位");
        }
        if (!passwordComplex(pwd)) {
            throw new RuntimeException("密码需包含大写字母、小写字母、数字、特殊符号中的至少3类");
        }
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(pwd));
        user.setRealName(dto.getRealName());
        user.setPhone(dto.getPhone());
        user.setDept(dto.getDept());
        user.setRole("engineer");
        user.setStatus(1);
        baseMapper.insert(user);
    }

    private boolean passwordComplex(String pwd) {
        int kinds = 0;
        if (pwd.matches(".*[A-Z].*")) kinds++;
        if (pwd.matches(".*[a-z].*")) kinds++;
        if (pwd.matches(".*[0-9].*")) kinds++;
        if (pwd.matches(".*[^a-zA-Z0-9].*")) kinds++;
        return kinds >= 3;
    }
}
