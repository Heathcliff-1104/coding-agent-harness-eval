package com.koolearn.bms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.koolearn.bms.entity.Material;
import com.koolearn.bms.service.BomMatchService;
import com.koolearn.bms.service.MaterialService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * BOM 匹配实现：
 * - 充足: available >= need
 * - 不足: 0 < available < need
 * - 缺料: 物料不存在 或 stock = 0
 * - 被占用: available = 0 但 stock > 0（全部被未完成单据锁定）
 * available = stock - lockStock（lockStock 为空按 0 处理）
 */
@Service
public class BomMatchServiceImpl implements BomMatchService {

    private final MaterialService materialService;

    public BomMatchServiceImpl(MaterialService materialService) {
        this.materialService = materialService;
    }

    @Override
    public List<Map<String, Object>> match(List<Map<String, Object>> items) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (items == null) {
            return result;
        }
        for (Map<String, Object> item : items) {
            result.add(matchOne(item));
        }
        return result;
    }

    private Map<String, Object> matchOne(Map<String, Object> item) {
        Map<String, Object> row = new HashMap<>();
        row.put("materialCode", str(item.get("materialCode")));
        row.put("materialName", str(item.get("materialName")));
        row.put("packageType", str(item.get("packageType")));
        row.put("specModel", str(item.get("specModel")));
        row.put("batchNo", str(item.get("batchNo")));
        row.put("needNum", item.get("needNum"));

        BigDecimal need = toDecimal(item.get("needNum"));
        if (need == null) need = BigDecimal.ZERO;
        row.put("needNum", need);

        String code = str(item.get("materialCode"));
        String name = str(item.get("materialName"));
        Material mat = findMaterial(code, name);

        row.put("status", "缺料");
        row.put("currentStock", BigDecimal.ZERO);
        row.put("available", BigDecimal.ZERO);
        row.put("shortage", need);

        if (mat != null) {
            BigDecimal stock = mat.getStock() != null ? mat.getStock() : BigDecimal.ZERO;
            BigDecimal lockStock = mat.getLockStock() != null ? mat.getLockStock() : BigDecimal.ZERO;
            BigDecimal available = stock.subtract(lockStock);
            row.put("materialId", mat.getId());
            row.put("currentStock", stock);
            row.put("available", available);
            row.put("lockStock", lockStock);
            row.put("materialCode", mat.getMaterialCode());
            row.put("materialName", mat.getMaterialName());
            row.put("packageType", mat.getPackageType());
            row.put("specModel", mat.getSpecModel());
            row.put("manufacturerBatch", mat.getManufacturerBatch());

            if (available.compareTo(need) >= 0) {
                row.put("status", "充足");
                row.put("shortage", BigDecimal.ZERO);
            } else if (available.compareTo(BigDecimal.ZERO) == 0 && stock.compareTo(BigDecimal.ZERO) > 0) {
                row.put("status", "被占用");
                row.put("shortage", need.subtract(available));
            } else if (available.compareTo(BigDecimal.ZERO) > 0) {
                row.put("status", "不足");
                row.put("shortage", need.subtract(available));
            } else {
                row.put("status", "缺料");
                row.put("shortage", need);
            }
        }
        return row;
    }

    private Material findMaterial(String code, String name) {
        if (StringUtils.hasText(code)) {
            List<Material> byCode = materialService.list(new LambdaQueryWrapper<Material>()
                    .eq(Material::getMaterialCode, code).last("limit 1"));
            if (!byCode.isEmpty()) return byCode.get(0);
        }
        if (StringUtils.hasText(name)) {
            List<Material> byName = materialService.list(new LambdaQueryWrapper<Material>()
                    .eq(Material::getMaterialName, name).last("limit 1"));
            if (!byName.isEmpty()) return byName.get(0);
        }
        return null;
    }

    private String str(Object o) {
        return o == null ? "" : o.toString();
    }

    private BigDecimal toDecimal(Object o) {
        if (o == null) return null;
        if (o instanceof BigDecimal) return (BigDecimal) o;
        try {
            return new BigDecimal(o.toString());
        } catch (Exception e) {
            return null;
        }
    }
}
