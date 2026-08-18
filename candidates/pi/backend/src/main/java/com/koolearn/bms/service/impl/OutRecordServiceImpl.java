package com.koolearn.bms.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.koolearn.bms.entity.OutRecord;
import com.koolearn.bms.mapper.OutRecordMapper;
import com.koolearn.bms.service.OutRecordService;
import org.springframework.stereotype.Service;

@Service
public class OutRecordServiceImpl extends ServiceImpl<OutRecordMapper, OutRecord> implements OutRecordService {
}