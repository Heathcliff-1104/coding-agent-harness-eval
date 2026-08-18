package com.koolearn.bms.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.koolearn.bms.entity.SysConfig;

public interface SysConfigService extends IService<SysConfig> {
    String get(String key, String defaultValue);
    void set(String key, String value, String description);
}
