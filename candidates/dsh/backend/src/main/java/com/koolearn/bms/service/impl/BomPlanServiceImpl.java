package com.koolearn.bms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.koolearn.bms.entity.BomPlan;
import com.koolearn.bms.entity.Material;
import com.koolearn.bms.entity.OutRecord;
import com.koolearn.bms.mapper.BomPlanMapper;
import com.koolearn.bms.mapper.OutRecordMapper;
import com.koolearn.bms.service.BomPlanService;
import com.koolearn.bms.service.MaterialService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class BomPlanServiceImpl implements BomPlanService {

    private final MaterialService materialService;
    private final BomPlanMapper bomPlanMapper;
    private final OutRecordMapper outRecordMapper;

    public BomPlanServiceImpl(MaterialService materialService, BomPlanMapper bomPlanMapper,
                              OutRecordMapper outRecordMapper) {
        this.materialService = materialService;
        this.bomPlanMapper = bomPlanMapper;
        this.outRecordMapper = outRecordMapper;
    }

    @Override
    public List<Map<String, Object>> matchBom(List<Map<String, Object>> items) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (items == null) return result;
        for (Map<String, Object> row : items) {
            Map<String, Object> out = new HashMap<>(row);
            String code = str(row.get("materialCode"));
            String name = str(row.get("materialName"));
            BigDecimal need = dec(row.get("needNum"));
            if (need == null || need.compareTo(BigDecimal.ZERO) <= 0) need = BigDecimal.ONE;

            Material mat = null;
            if (code != null && !code.isEmpty()) {
                mat = materialService.getOne(new LambdaQueryWrapper<Material>()
                        .eq(Material::getMaterialCode, code).last("limit 1"));
            }
            if (mat == null && name != null && !name.isEmpty()) {
                mat = materialService.getOne(new LambdaQueryWrapper<Material>()
                        .eq(Material::getMaterialName, name).last("limit 1"));
            }
            if (mat == null) {
                out.put("materialId", null);
                out.put("currentStock", 0);
                out.put("shortage", need);
                out.put("stockStatus", "out_of_stock");
                out.put("stockStatusText", "缺料");
                result.add(out);
                continue;
            }
            BigDecimal stock = mat.getStock() == null ? BigDecimal.ZERO : mat.getStock();
            BigDecimal lock = mat.getLockStock() == null ? BigDecimal.ZERO : mat.getLockStock();
            BigDecimal available = stock.subtract(lock);
            out.put("materialId", mat.getId());
            out.put("materialCode", mat.getMaterialCode());
            out.put("materialName", mat.getMaterialName());
            out.put("packageType", mat.getPackageType());
            out.put("specModel", mat.getSpecModel());
            out.put("currentStock", stock);
            out.put("lockStock", lock);
            out.put("shortage", available.compareTo(need) < 0 ? need.subtract(available) : BigDecimal.ZERO);
            if (available.compareTo(need) >= 0) {
                out.put("stockStatus", "sufficient");
                out.put("stockStatusText", "库存充足");
            } else if (stock.compareTo(BigDecimal.ZERO) > 0) {
                out.put("stockStatus", "insufficient");
                out.put("stockStatusText", "库存不足");
            } else if (lock.compareTo(BigDecimal.ZERO) > 0) {
                out.put("stockStatus", "occupied");
                out.put("stockStatusText", "被占用");
            } else {
                out.put("stockStatus", "out_of_stock");
                out.put("stockStatusText", "缺料");
            }
            // 已出库物料：备注显示最近出库单号
            OutRecord last = outRecordMapper.selectOne(new LambdaQueryWrapper<OutRecord>()
                    .eq(OutRecord::getMaterialId, mat.getId())
                    .orderByDesc(OutRecord::getOutTime)
                    .last("limit 1"));
            if (last != null) {
                out.put("remark", "已出库，出库单:" + last.getOutboundCode());
            } else {
                out.put("remark", null);
            }
            result.add(out);
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String savePlan(String bomVersion, List<Map<String, Object>> matchedItems, String createBy) {
        String planNo = "PLAN-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        if (matchedItems != null) {
            for (Map<String, Object> row : matchedItems) {
                BomPlan plan = new BomPlan();
                plan.setPlanNo(planNo);
                plan.setBomVersion(bomVersion);
                plan.setMaterialId(row.get("materialId") == null ? null : Long.valueOf(row.get("materialId").toString()));
                plan.setMaterialCode(str(row.get("materialCode")));
                plan.setMaterialName(str(row.get("materialName")));
                plan.setPackageType(str(row.get("packageType")));
                plan.setValueData(str(row.get("valueData")));
                plan.setSpecModel(str(row.get("specModel")));
                plan.setBatchNo(str(row.get("batchNo")));
                plan.setNeedNum(dec(row.get("needNum")));
                plan.setCurrentStock(dec(row.get("currentStock")));
                plan.setShortage(dec(row.get("shortage")));
                plan.setStockStatus(str(row.get("stockStatus")));
                plan.setRemark(str(row.get("remark")));
                plan.setCreateBy(createBy);
                plan.setCreateTime(LocalDateTime.now());
                bomPlanMapper.insert(plan);
            }
        }
        return planNo;
    }

    @Override
    public IPage<BomPlan> pagePlans(Long pageNum, Long pageSize, String bomVersion) {
        Page<BomPlan> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<BomPlan> qw = new LambdaQueryWrapper<>();
        if (bomVersion != null && !bomVersion.isEmpty()) {
            qw.eq(BomPlan::getBomVersion, bomVersion);
        }
        qw.orderByDesc(BomPlan::getCreateTime);
        return bomPlanMapper.selectPage(page, qw);
    }

    private String str(Object o) {
        return o == null ? null : o.toString();
    }

    private BigDecimal dec(Object o) {
        if (o == null) return null;
        if (o instanceof Number) return new BigDecimal(o.toString());
        try {
            return new BigDecimal(o.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
