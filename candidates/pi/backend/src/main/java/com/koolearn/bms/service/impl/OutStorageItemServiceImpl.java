package com.koolearn.bms.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.koolearn.bms.entity.OutStorageItem;
import com.koolearn.bms.mapper.OutStorageItemMapper;
import com.koolearn.bms.service.OutStorageItemService;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class OutStorageItemServiceImpl extends ServiceImpl<OutStorageItemMapper, OutStorageItem> implements OutStorageItemService {

    @Override
    public List<OutStorageItem> selectByOutboundId(Long outboundId) {
        return baseMapper.selectByOutboundId(outboundId);
    }
}
