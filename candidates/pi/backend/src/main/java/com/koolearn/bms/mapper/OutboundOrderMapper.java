package com.koolearn.bms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.koolearn.bms.entity.OutboundOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface OutboundOrderMapper extends BaseMapper<OutboundOrder> {

    int updateOrderStatus(@Param("id") Long id, @Param("orderStatus") Integer orderStatus);

    /** 原子状态流转：待审批(0) -> 已出库(1)，防止 TOCTOU */
    int atomicConfirm(@Param("id") Long id);

    /** 原子状态流转：待审批(0) -> 已驳回(2)，防止 TOCTOU */
    int atomicReject(@Param("id") Long id);

    /** 回调同意：仅当仍处于待审批(0)时保持 0（防旧回调回写已处理单据） */
    int approveFromCallback(@Param("id") Long id);

    OutboundOrder selectByDingInstanceId(@Param("dingInstanceId") String dingInstanceId);
}
