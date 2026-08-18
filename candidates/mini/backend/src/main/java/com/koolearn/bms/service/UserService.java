package com.koolearn.bms.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.koolearn.bms.dto.UserRegisterDTO;
import com.koolearn.bms.entity.User;

public interface UserService extends IService<User> {

    User login(String username, String password);

    void register(UserRegisterDTO dto);
}
