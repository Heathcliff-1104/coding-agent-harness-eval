package com.koolearn.bms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.koolearn.bms.dto.StockFlowDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface StockFlowMapper extends BaseMapper<StockFlowDTO> {

    /** 合并查询入库/出库流水（SQL 层过滤 + 分页） */
    IPage<StockFlowDTO> selectFlowPage(Page<StockFlowDTO> page,
                                       @Param("keyword") String keyword,
                                       @Param("materialCode") String materialCode,
                                       @Param("materialId") Long materialId,
                                       @Param("startTime") String startTime,
                                       @Param("endTime") String endTime,
                                       @Param("type") String type);

    /** 全量导出用 */
    List<StockFlowDTO> selectFlowAll(@Param("keyword") String keyword,
                                     @Param("materialCode") String materialCode,
                                     @Param("materialId") Long materialId,
                                     @Param("startTime") String startTime,
                                     @Param("endTime") String endTime,
                                     @Param("type") String type);
}
