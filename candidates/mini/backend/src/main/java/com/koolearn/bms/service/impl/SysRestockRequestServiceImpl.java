package com.koolearn.bms.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.koolearn.bms.entity.SysRestockRequest;
import com.koolearn.bms.mapper.SysRestockRequestMapper;
import com.koolearn.bms.service.SysRestockRequestService;
import org.springframework.stereotype.Service;

@Service
public class SysRestockRequestServiceImpl extends ServiceImpl<SysRestockRequestMapper, SysRestockRequest>
        implements SysRestockRequestService {
}
