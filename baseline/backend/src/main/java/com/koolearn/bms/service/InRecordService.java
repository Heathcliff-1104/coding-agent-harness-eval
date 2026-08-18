package com.koolearn.bms.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.koolearn.bms.entity.InRecord;
import java.util.List;

public interface InRecordService extends IService<InRecord> {
    List<InRecord> selectByDate(String start, String end);
    IPage<InRecord> pageQuery(Long pageNum, Long pageSize, String start, String end, String keyword, String billNo);
}