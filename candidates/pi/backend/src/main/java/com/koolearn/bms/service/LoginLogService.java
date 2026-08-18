package com.koolearn.bms.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.koolearn.bms.entity.LoginLog;

public interface LoginLogService extends IService<LoginLog> {
    void record(String username, String ip, String device, Integer result);
}
