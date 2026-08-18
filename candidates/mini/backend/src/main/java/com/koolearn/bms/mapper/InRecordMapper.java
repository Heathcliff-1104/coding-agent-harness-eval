package com.koolearn.bms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.koolearn.bms.entity.InRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface InRecordMapper extends BaseMapper<InRecord> {
    List<InRecord> selectByDate(@Param("start") String start, @Param("end") String end);

    IPage<InRecord> selectPageByCondition(Page<InRecord> page,
                                          @Param("start") String start,
                                          @Param("end") String end,
                                          @Param("keyword") String keyword,
                                          @Param("billNo") String billNo);
}