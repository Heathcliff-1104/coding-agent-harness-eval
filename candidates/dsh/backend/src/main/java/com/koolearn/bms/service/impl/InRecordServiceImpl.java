package com.koolearn.bms.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.koolearn.bms.entity.InRecord;
import com.koolearn.bms.mapper.InRecordMapper;
import com.koolearn.bms.service.InRecordService;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class InRecordServiceImpl extends ServiceImpl<InRecordMapper, InRecord> implements InRecordService {

    @Override
    public List<InRecord> selectByDate(String start, String end) {
        return baseMapper.selectByDate(start, end);
    }

    @Override
    public IPage<InRecord> pageQuery(Long pageNum, Long pageSize, String start, String end, String keyword, String billNo) {
        Page<InRecord> page = new Page<>(pageNum, pageSize);
        return baseMapper.selectPageByCondition(page, start, end, keyword, billNo);
    }
}