package com.koolearn.bms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.koolearn.bms.entity.InboundOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

@Mapper
public interface InboundOrderMapper extends BaseMapper<InboundOrder> {

    int insert(InboundOrder inboundOrder);

    InboundOrder selectById(@Param("id") Long id);

    InboundOrder selectByDingInstanceId(@Param("dingInstanceId") String dingInstanceId);

    int updateOrderStatus(@Param("id") Long id, @Param("orderStatus") Integer orderStatus);

    // 新增：分页多条件查询入库单
    IPage<InboundOrder> selectOrderPage(Page<InboundOrder> page,
                                        @Param("billNo") String billNo,
                                        @Param("supplier") String supplier,
                                        @Param("status") Integer status,
                                        @Param("keyword") String keyword);
}