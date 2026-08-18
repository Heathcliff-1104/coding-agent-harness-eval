package com.koolearn.bms.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.koolearn.bms.entity.LoginLog;
import com.koolearn.bms.mapper.LoginLogMapper;
import com.koolearn.bms.service.LoginLogService;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
public class LoginLogServiceImpl extends ServiceImpl<LoginLogMapper, LoginLog> implements LoginLogService {

    @Override
    public void record(String username, String ip, String device, Integer result) {
        LoginLog log = new LoginLog();
        log.setUsername(username);
        log.setLoginIp(ip);
        log.setDeviceInfo(device);
        log.setLoginTime(LocalDateTime.now());
        log.setLoginResult(result);
        baseMapper.insert(log);
    }
}
