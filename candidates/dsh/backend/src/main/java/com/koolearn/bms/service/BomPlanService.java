package com.koolearn.bms.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.koolearn.bms.entity.BomPlan;

import java.util.List;
import java.util.Map;

/**
 * 生产领料 BOM 匹配与备料计划单（需求 2.3.2）。
 */
public interface BomPlanService {

    /**
     * 逐项匹配库存，返回每项物料状态（库存充足/不足/缺料/被占用）。
     * items 元素需包含 materialCode/materialName/packageType/specModel/batchNo/needNum。
     */
    List<Map<String, Object>> matchBom(List<Map<String, Object>> items);

    /** 将匹配结果保存为备料计划单（记录BOM版本，支持重复BOM直接匹配历史）。 */
    String savePlan(String bomVersion, List<Map<String, Object>> matchedItems, String createBy);

    IPage<BomPlan> pagePlans(Long pageNum, Long pageSize, String bomVersion);
}
