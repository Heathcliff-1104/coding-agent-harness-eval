package com.koolearn.bms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.koolearn.bms.dto.BomItemDTO;
import com.koolearn.bms.dto.BomMatchResultDTO;
import com.koolearn.bms.entity.*;
import com.koolearn.bms.mapper.*;
import com.koolearn.bms.service.BomService;
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
public class BomServiceImpl implements BomService {

    private final BomMapper bomMapper;
    private final BomItemMapper bomItemMapper;
    private final BomMatchHistoryMapper matchHistoryMapper;
    private final PickPlanMapper pickPlanMapper;
    private final PickPlanItemMapper pickPlanItemMapper;
    private final MaterialService materialService;

    public BomServiceImpl(BomMapper bomMapper, BomItemMapper bomItemMapper,
                          BomMatchHistoryMapper matchHistoryMapper,
                          PickPlanMapper pickPlanMapper, PickPlanItemMapper pickPlanItemMapper,
                          MaterialService materialService) {
        this.bomMapper = bomMapper;
        this.bomItemMapper = bomItemMapper;
        this.matchHistoryMapper = matchHistoryMapper;
        this.pickPlanMapper = pickPlanMapper;
        this.pickPlanItemMapper = pickPlanItemMapper;
        this.materialService = materialService;
    }

    @Override
    public List<BomMatchResultDTO> match(List<BomItemDTO> items) {
        List<BomMatchResultDTO> results = new ArrayList<>();
        if (items == null) return results;
        for (BomItemDTO dto : items) {
            Material mat = resolveMaterial(dto);
            BomMatchResultDTO result = new BomMatchResultDTO();
            if (mat == null) {
                result.setMaterialCode(dto.getMaterialCode());
                result.setMaterialName(dto.getMaterialName());
                result.setPackageType(dto.getPackageType());
                result.setValueData(dto.getValueData());
                result.setSpecModel(dto.getSpecModel());
                result.setBatchNo(dto.getBatchNo());
                result.setNeedNum(dto.getNeedNum());
                result.setCurrentStock(BigDecimal.ZERO);
                result.setShortage(dto.getNeedNum());
                result.setStockStatus("out_of_stock");
            } else {
                result.setMaterialId(mat.getId());
                result.setMaterialCode(mat.getMaterialCode());
                result.setMaterialName(mat.getMaterialName());
                result.setPackageType(mat.getPackageType());
                result.setValueData(mat.getValueData());
                result.setSpecModel(mat.getSpecModel());
                result.setBatchNo(dto.getBatchNo());
                result.setNeedNum(dto.getNeedNum());
                result.setCurrentStock(mat.getStock());
                BigDecimal available = mat.getStock().subtract(mat.getLockStock() != null ? mat.getLockStock() : BigDecimal.ZERO);
                if (available.compareTo(dto.getNeedNum()) >= 0) {
                    result.setStockStatus("sufficient");
                    result.setShortage(BigDecimal.ZERO);
                } else if (available.compareTo(BigDecimal.ZERO) > 0) {
                    result.setStockStatus("insufficient");
                    result.setShortage(dto.getNeedNum().subtract(available));
                } else if (mat.getLockStock() != null && mat.getLockStock().compareTo(BigDecimal.ZERO) > 0) {
                    result.setStockStatus("occupied");
                    result.setShortage(dto.getNeedNum());
                } else {
                    result.setStockStatus("out_of_stock");
                    result.setShortage(dto.getNeedNum());
                }
            }
            results.add(result);
        }
        return results;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> importBom(String bomName, String creator, List<BomItemDTO> items) {
        Bom bom = new Bom();
        bom.setBomCode("BOM" + System.currentTimeMillis());
        bom.setBomName(bomName == null ? "未命名BOM" : bomName);
        bom.setVersion("v1");
        bom.setStatus(1);
        bom.setCreator(creator);
        bom.setCreateTime(LocalDateTime.now());
        bom.setUpdateTime(LocalDateTime.now());
        bomMapper.insert(bom);

        for (BomItemDTO dto : items) {
            BomItem item = new BomItem();
            Material mat = resolveMaterial(dto);
            item.setBomId(bom.getId());
            item.setMaterialId(mat == null ? null : mat.getId());
            item.setMaterialCode(dto.getMaterialCode());
            item.setMaterialName(dto.getMaterialName());
            item.setPackageType(dto.getPackageType());
            item.setValueData(dto.getValueData());
            item.setSpecModel(dto.getSpecModel());
            item.setBatchNo(dto.getBatchNo());
            item.setNeedNum(dto.getNeedNum());
            item.setRemark(dto.getMaterialName() == null ? "" : "");
            bomItemMapper.insert(item);
        }

        List<BomMatchResultDTO> matched = match(items);
        for (BomMatchResultDTO r : matched) {
            BomMatchHistory history = new BomMatchHistory();
            history.setBomId(bom.getId());
            history.setBomVersion(bom.getVersion());
            history.setMaterialCode(r.getMaterialCode());
            history.setMaterialName(r.getMaterialName());
            history.setStockStatus(r.getStockStatus());
            history.setCurrentStock(r.getCurrentStock());
            history.setNeedNum(r.getNeedNum());
            history.setShortage(r.getShortage());
            history.setMatchTime(LocalDateTime.now());
            matchHistoryMapper.insert(history);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("bomId", bom.getId());
        result.put("bomCode", bom.getBomCode());
        result.put("version", bom.getVersion());
        result.put("matchResults", matched);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String savePlan(Long bomId, String planNo, String creator, List<BomItemDTO> items, String remark) {
        Bom bom = bomId != null ? bomMapper.selectById(bomId) : null;
        PickPlan plan = new PickPlan();
        plan.setPlanNo(planNo == null || planNo.isEmpty() ? "PLAN" + System.currentTimeMillis() : planNo);
        plan.setBomId(bomId);
        plan.setBomVersion(bom == null ? "" : bom.getVersion());
        plan.setCreator(creator);
        plan.setRemark(remark);
        plan.setCreateTime(LocalDateTime.now());
        pickPlanMapper.insert(plan);

        List<BomMatchResultDTO> matched = match(items);
        for (BomMatchResultDTO r : matched) {
            PickPlanItem item = new PickPlanItem();
            item.setPlanId(plan.getId());
            item.setMaterialCode(r.getMaterialCode());
            item.setMaterialName(r.getMaterialName());
            item.setPackageType(r.getPackageType());
            item.setValueData(r.getValueData());
            item.setSpecModel(r.getSpecModel());
            item.setStock(r.getCurrentStock());
            item.setNeedNum(r.getNeedNum());
            item.setSupplementNum(r.getShortage());
            item.setRemark(r.getStockStatus());
            pickPlanItemMapper.insert(item);
        }
        return plan.getPlanNo();
    }

    @Override
    public List<Map<String, Object>> listVersions() {
        List<Bom> boms = bomMapper.selectList(new LambdaQueryWrapper<Bom>().orderByDesc(Bom::getCreateTime));
        List<Map<String, Object>> list = new ArrayList<>();
        for (Bom b : boms) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", b.getId());
            map.put("bomCode", b.getBomCode());
            map.put("bomName", b.getBomName());
            map.put("version", b.getVersion());
            map.put("creator", b.getCreator());
            map.put("createTime", b.getCreateTime());
            list.add(map);
        }
        return list;
    }

    private Material resolveMaterial(BomItemDTO dto) {
        if (dto.getMaterialId() != null) {
            return materialService.getById(dto.getMaterialId());
        }
        if (dto.getMaterialCode() != null && !dto.getMaterialCode().trim().isEmpty()) {
            Material m = materialService.getByCode(dto.getMaterialCode());
            if (m != null) return m;
        }
        if (dto.getMaterialName() != null && !dto.getMaterialName().trim().isEmpty()) {
            List<Material> list = materialService.list(new LambdaQueryWrapper<Material>()
                    .eq(Material::getMaterialName, dto.getMaterialName()).last("limit 1"));
            if (!list.isEmpty()) return list.get(0);
        }
        return null;
    }
}
