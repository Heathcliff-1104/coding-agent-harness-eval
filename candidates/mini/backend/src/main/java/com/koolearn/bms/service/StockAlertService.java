package com.koolearn.bms.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.koolearn.bms.entity.StockAlert;

public interface StockAlertService extends IService<StockAlert> {
    void scanAndAlert();
}
