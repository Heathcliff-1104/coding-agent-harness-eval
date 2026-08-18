package com.koolearn.bms.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.koolearn.bms.entity.OutStorageItem;
import java.util.List;

public interface OutStorageItemService extends IService<OutStorageItem> {
    List<OutStorageItem> selectByOutboundId(Long outboundId);
}
