package com.koolearn.bms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.koolearn.bms.entity.SysConfig;
import com.koolearn.bms.mapper.SysConfigMapper;
import com.koolearn.bms.service.SysConfigService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class SysConfigServiceImpl extends ServiceImpl<SysConfigMapper, SysConfig> implements SysConfigService {

    @Override
    public String get(String key, String defaultValue) {
        SysConfig cfg = getOne(new LambdaQueryWrapper<SysConfig>().eq(SysConfig::getConfigKey, key));
        return cfg == null || cfg.getConfigValue() == null ? defaultValue : cfg.getConfigValue();
    }

    @Override
    public void set(String key, String value, String description) {
        SysConfig cfg = getOne(new LambdaQueryWrapper<SysConfig>().eq(SysConfig::getConfigKey, key));
        if (cfg == null) {
            cfg = new SysConfig();
            cfg.setConfigKey(key);
            cfg.setConfigValue(value);
            cfg.setDescription(description);
            cfg.setUpdateTime(LocalDateTime.now());
            save(cfg);
        } else {
            cfg.setConfigValue(value);
            cfg.setDescription(description);
            cfg.setUpdateTime(LocalDateTime.now());
            updateById(cfg);
        }
    }
}
