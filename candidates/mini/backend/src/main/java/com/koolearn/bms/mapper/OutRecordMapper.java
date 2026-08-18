package com.koolearn.bms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.koolearn.bms.entity.OutRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface OutRecordMapper extends BaseMapper<OutRecord> {
    IPage<OutRecord> selectPageByCondition(Page<OutRecord> page,
                                           @Param("outboundCode") String outboundCode,
                                           @Param("materialId") Long materialId,
                                           @Param("startTime") String startTime,
                                           @Param("endTime") String endTime,
                                           @Param("keyword") String keyword);
}
