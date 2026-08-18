package com.koolearn.bms.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.koolearn.bms.dto.OutboundOrderDTO;
import com.koolearn.bms.dto.OutStorageItemDTO;
import com.koolearn.bms.entity.BomHeader;
import com.koolearn.bms.entity.BomItem;
import com.koolearn.bms.entity.Material;
import com.koolearn.bms.mapper.BomHeaderMapper;
import com.koolearn.bms.mapper.BomItemMapper;
import com.koolearn.bms.service.BomMatchService;
import com.koolearn.bms.service.MaterialService;
import com.koolearn.bms.service.OutboundOrderService;
import com.koolearn.bms.util.Result;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * BOM 管理（生产领料）：
 * POST /outbound/bom/import     上传Excel解析BOM
 * POST /outbound/bom/match      逐项匹配库存状态
 * POST /outbound/bom/plan       保存备料计划单 + 生成出库草稿
 * GET  /outbound/bom/history    历史BOM（模糊查询）
 * GET  /outbound/bom/history/{id} 复用历史BOM（重新匹配）
 */
@RestController
@RequestMapping("/outbound/bom")
public class BomController {

    private final BomMatchService bomMatchService;
    private final MaterialService materialService;
    private final OutboundOrderService outboundOrderService;
    private final BomHeaderMapper bomHeaderMapper;
    private final BomItemMapper bomItemMapper;

    public BomController(BomMatchService bomMatchService, MaterialService materialService,
                         OutboundOrderService outboundOrderService,
                         BomHeaderMapper bomHeaderMapper, BomItemMapper bomItemMapper) {
        this.bomMatchService = bomMatchService;
        this.materialService = materialService;
        this.outboundOrderService = outboundOrderService;
        this.bomHeaderMapper = bomHeaderMapper;
        this.bomItemMapper = bomItemMapper;
    }

    @PostMapping("/import")
    public Result<List<Map<String, Object>>> importBom(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return Result.fail("请上传BOM文件");
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        try (Workbook wb = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = wb.getSheetAt(0);
            if (sheet == null || sheet.getLastRowNum() < 0) {
                return Result.fail("BOM文件为空");
            }
            Row header = sheet.getRow(0);
            int codeIdx = -1, nameIdx = -1, pkgIdx = -1, specIdx = -1, batchIdx = -1, numIdx = -1;
            for (int c = 0; c < header.getLastCellNum(); c++) {
                String h = header.getCell(c) == null ? "" : header.getCell(c).getStringCellValue().trim();
                if (codeIdx < 0 && (h.contains("物料编码") || h.contains("编码") || h.equalsIgnoreCase("code"))) codeIdx = c;
                if (nameIdx < 0 && (h.contains("物料名称") || h.contains("名称") || h.equalsIgnoreCase("name"))) nameIdx = c;
                if (pkgIdx < 0 && (h.contains("封装") || h.contains("package"))) pkgIdx = c;
                if (specIdx < 0 && (h.contains("规格型号") || h.contains("型号") || h.contains("spec"))) specIdx = c;
                if (batchIdx < 0 && (h.contains("批次") || h.contains("batch"))) batchIdx = c;
                if (numIdx < 0 && (h.contains("数量") || h.contains("num") || h.contains("need") || h.contains("用量"))) numIdx = c;
            }
            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("materialCode", cellStr(row, codeIdx));
                item.put("materialName", cellStr(row, nameIdx));
                item.put("packageType", cellStr(row, pkgIdx));
                item.put("specModel", cellStr(row, specIdx));
                item.put("batchNo", cellStr(row, batchIdx));
                item.put("needNum", cellNum(row, numIdx));
                if (StringUtils.hasText(cellStr(row, codeIdx)) || StringUtils.hasText(cellStr(row, nameIdx))) {
                    rows.add(item);
                }
            }
            return Result.success(rows);
        } catch (Exception e) {
            return Result.fail("BOM解析失败: " + e.getMessage());
        }
    }

    private String cellStr(Row row, int idx) {
        if (idx < 0 || row.getCell(idx) == null) return "";
        try {
            return row.getCell(idx).getStringCellValue().trim();
        } catch (Exception e) {
            try {
                return String.valueOf(row.getCell(idx).getNumericCellValue()).trim();
            } catch (Exception e2) {
                return "";
            }
        }
    }

    private BigDecimal cellNum(Row row, int idx) {
        if (idx < 0 || row.getCell(idx) == null) return BigDecimal.ZERO;
        try {
            return BigDecimal.valueOf(row.getCell(idx).getNumericCellValue());
        } catch (Exception e) {
            try {
                return new BigDecimal(row.getCell(idx).getStringCellValue().trim());
            } catch (Exception e2) {
                return BigDecimal.ZERO;
            }
        }
    }

    @PostMapping("/match")
    public Result<List<Map<String, Object>>> match(@RequestBody List<Map<String, Object>> items) {
        return Result.success(bomMatchService.match(items));
    }

    @PostMapping("/plan")
    public Result<Long> savePlan(@RequestBody Map<String, Object> body,
                                 @RequestAttribute(value = "username", required = false) String operator) {
        String bomName = body.get("bomName") != null ? body.get("bomName").toString() : "备料计划";
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) body.get("items");
        if (items == null) items = new ArrayList<>();

        BomHeader header = new BomHeader();
        header.setBomNo("BOM" + System.currentTimeMillis());
        header.setBomName(bomName);
        header.setVersion(1);
        header.setRepeatFlag(0);
        header.setCreateUser(operator != null ? operator : "unknown");
        header.setCreateTime(LocalDateTime.now());
        bomHeaderMapper.insert(header);

        List<OutStorageItemDTO> draftItems = new ArrayList<>();
        for (Map<String, Object> item : items) {
            String code = item.get("materialCode") != null ? item.get("materialCode").toString() : "";
            BigDecimal need = toDec(item.get("needNum"));
            BomItem bi = new BomItem();
            bi.setBomId(header.getId());
            bi.setMaterialCode(code);
            bi.setMaterialName(item.get("materialName") != null ? item.get("materialName").toString() : "");
            bi.setPackageType(item.get("packageType") != null ? item.get("packageType").toString() : "");
            bi.setSpecModel(item.get("specModel") != null ? item.get("specModel").toString() : "");
            bi.setBatchNo(item.get("batchNo") != null ? item.get("batchNo").toString() : "");
            bi.setNeedNum(need);
            bi.setRemark(item.get("remark") != null ? item.get("remark").toString() : "");
            bomItemMapper.insert(bi);

            Material mat = findMaterial(code, item.get("materialName") != null ? item.get("materialName").toString() : "");
            if (mat != null) {
                bi.setMaterialId(mat.getId());
                bomItemMapper.updateById(bi);
                BigDecimal available = mat.getStock().subtract(mat.getLockStock() != null ? mat.getLockStock() : BigDecimal.ZERO);
                if (available.compareTo(need) >= 0) {
                    OutStorageItemDTO di = new OutStorageItemDTO();
                    di.setMaterialId(mat.getId());
                    di.setMaterialCode(mat.getMaterialCode());
                    di.setBatchNo(item.get("batchNo") != null ? item.get("batchNo").toString() : "");
                    di.setOutNum(need);
                    draftItems.add(di);
                }
            }
        }

        Long draftId = null;
        if (!draftItems.isEmpty()) {
            OutboundOrderDTO dto = new OutboundOrderDTO();
            dto.setOutType(1);
            dto.setApplyUser(operator != null ? operator : "unknown");
            dto.setRemark("备料计划单: " + header.getBomNo());
            dto.setItemList(draftItems);
            try {
                draftId = outboundOrderService.saveDraft(dto);
            } catch (Exception e) {
                // 备料计划仍保存，草稿创建失败不阻断（记录日志由调用方处理）
                draftId = null;
            }
        }
        return Result.success(draftId);
    }

    @GetMapping("/history")
    public Result<List<Map<String, Object>>> history(@RequestParam(required = false) String keyword) {
        List<BomHeader> headers;
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            List<Long> ids = bomItemMapper.selectList(new LambdaQueryWrapper<BomItem>()
                            .like(BomItem::getMaterialCode, kw).or().like(BomItem::getMaterialName, kw))
                    .stream().map(BomItem::getBomId).distinct().collect(java.util.stream.Collectors.toList());
            headers = bomHeaderMapper.selectList(new LambdaQueryWrapper<BomHeader>()
                    .and(w -> w.like(BomHeader::getBomNo, kw).or().like(BomHeader::getBomName, kw)
                            .or(!ids.isEmpty(), w2 -> w2.in(BomHeader::getId, ids)))
                    .orderByDesc(BomHeader::getCreateTime));
        } else {
            headers = bomHeaderMapper.selectList(new LambdaQueryWrapper<BomHeader>().orderByDesc(BomHeader::getCreateTime));
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (BomHeader h : headers) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", h.getId());
            m.put("bomNo", h.getBomNo());
            m.put("bomName", h.getBomName());
            m.put("version", h.getVersion());
            m.put("repeatFlag", h.getRepeatFlag());
            m.put("createUser", h.getCreateUser());
            m.put("createTime", h.getCreateTime() == null ? "" : h.getCreateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            result.add(m);
        }
        return Result.success(result);
    }

    @GetMapping("/history/{id}")
    public Result<Map<String, Object>> historyDetail(@PathVariable Long id) {
        BomHeader header = bomHeaderMapper.selectById(id);
        if (header == null) return Result.fail("BOM不存在");
        List<BomItem> items = bomItemMapper.selectList(new LambdaQueryWrapper<BomItem>().eq(BomItem::getBomId, id));
        Map<String, Object> result = new HashMap<>();
        result.put("id", header.getId());
        result.put("bomNo", header.getBomNo());
        result.put("bomName", header.getBomName());
        result.put("version", header.getVersion());
        result.put("items", items);
        return Result.success(result);
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

    private BigDecimal toDec(Object o) {
        if (o == null) return BigDecimal.ZERO;
        try {
            return new BigDecimal(o.toString());
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }
}
