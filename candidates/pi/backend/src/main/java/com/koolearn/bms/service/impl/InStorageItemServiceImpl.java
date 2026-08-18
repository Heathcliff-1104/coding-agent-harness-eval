package com.koolearn.bms.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.koolearn.bms.entity.InStorageItem;
import com.koolearn.bms.mapper.InStorageItemMapper;
import com.koolearn.bms.service.InStorageItemService;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class InStorageItemServiceImpl extends ServiceImpl<InStorageItemMapper, InStorageItem> implements InStorageItemService {

    @Override
    public List<InStorageItem> selectByInboundId(Long inboundId) {
        return baseMapper.selectByInboundId(inboundId);
    }
}