package com.koolearn.bms.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.koolearn.bms.entity.SysCisSyncLog;
import com.koolearn.bms.mapper.SysCisSyncLogMapper;
import com.koolearn.bms.service.SysCisSyncLogService;
import org.springframework.stereotype.Service;

@Service
public class SysCisSyncLogServiceImpl extends ServiceImpl<SysCisSyncLogMapper, SysCisSyncLog>
        implements SysCisSyncLogService {
}
