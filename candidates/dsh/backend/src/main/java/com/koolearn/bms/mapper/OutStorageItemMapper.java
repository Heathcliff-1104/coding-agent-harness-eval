package com.koolearn.bms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.koolearn.bms.entity.OutStorageItem;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface OutStorageItemMapper extends BaseMapper<OutStorageItem> {
    List<OutStorageItem> selectByOutboundId(Long outboundId);
    void deleteByOutboundId(Long outboundId);
}