package com.koolearn.bms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.koolearn.bms.entity.OutboundOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface OutboundOrderMapper extends BaseMapper<OutboundOrder> {

    int updateOrderStatus(@Param("id") Long id, @Param("orderStatus") Integer orderStatus);

    OutboundOrder selectByDingInstanceId(@Param("dingInstanceId") String dingInstanceId);
}
