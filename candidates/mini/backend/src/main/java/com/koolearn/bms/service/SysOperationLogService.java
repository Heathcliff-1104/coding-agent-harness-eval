package com.koolearn.bms.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.koolearn.bms.entity.SysOperationLog;

public interface SysOperationLogService extends IService<SysOperationLog> {
    void log(String username, String operation, String description, String ip);
}
