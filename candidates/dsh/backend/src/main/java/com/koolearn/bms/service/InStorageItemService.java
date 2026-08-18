package com.koolearn.bms.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.koolearn.bms.entity.InStorageItem;
import java.util.List;

public interface InStorageItemService extends IService<InStorageItem> {
    List<InStorageItem> selectByInboundId(Long inboundId);
}