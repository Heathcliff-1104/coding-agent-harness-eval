package com.koolearn.bms.service;

import java.util.List;
import java.util.Map;

/**
 * BOM 物料匹配服务（与库存状态判定）
 */
public interface BomMatchService {

    /**
     * 逐项匹配库存，返回每项：status(充足/不足/缺料/被占用), currentStock, available, shortage 及物料信息
     */
    List<Map<String, Object>> match(List<Map<String, Object>> items);
}
