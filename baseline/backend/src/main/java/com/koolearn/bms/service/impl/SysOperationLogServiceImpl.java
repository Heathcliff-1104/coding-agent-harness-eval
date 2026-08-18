package com.koolearn.bms.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.koolearn.bms.entity.SysOperationLog;
import com.koolearn.bms.mapper.SysOperationLogMapper;
import com.koolearn.bms.service.SysOperationLogService;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
public class SysOperationLogServiceImpl extends ServiceImpl<SysOperationLogMapper, SysOperationLog> implements SysOperationLogService {

    @Override
    public void log(String username, String operation, String description, String ip) {
        SysOperationLog log = new SysOperationLog();
        log.setUsername(username);
        log.setOperation(operation);
        log.setDescription(description);
        log.setIp(ip);
        log.setResult("成功");
        log.setCreateTime(LocalDateTime.now());
        baseMapper.insert(log);
    }
}
