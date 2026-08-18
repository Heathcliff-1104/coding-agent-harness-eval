package com.koolearn.bms.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.koolearn.bms.entity.InStorageItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface InStorageItemMapper extends BaseMapper<InStorageItem> {
    void insertBatch(@Param("list") List<InStorageItem> list);
    List<InStorageItem> selectByInboundId(@Param("inboundId") Long inboundId);
    //根据入库单id删除旧明细（草稿编辑用）
    int deleteByInboundId(@Param("inboundId") Long inboundId);
}