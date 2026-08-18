package com.koolearn.bms.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.koolearn.bms.entity.User;

public interface UserService extends IService<User> {

    User login(String username, String password);

    void register(User user);

    /** 管理员创建用户：可指定角色/状态，手机号可选。 */
    void registerByAdmin(User user);
}
