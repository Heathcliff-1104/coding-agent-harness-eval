package com.koolearn.bms.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.koolearn.bms.annotation.RequireRole;
import com.koolearn.bms.dto.StockFlowDTO;
import com.koolearn.bms.entity.InRecord;
import com.koolearn.bms.entity.OutRecord;
import com.koolearn.bms.service.InRecordService;
import com.koolearn.bms.service.OutRecordService;
import com.koolearn.bms.util.ExcelExportUtil;
import com.koolearn.bms.util.Result;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 库存流水（需求 2.4.4）：合并入库/出库（退库入库存为入库）记录，
 * 支持按物料、时间、操作类型筛选，导出 Excel。
 */
@RestController
@RequestMapping("/stockFlow")
@RequireRole({"admin", "warehouse"})
public class StockFlowController {

    private final InRecordService inRecordService;
    private final OutRecordService outRecordService;

    public StockFlowController(InRecordService inRecordService, OutRecordService outRecordService) {
        this.inRecordService = inRecordService;
        this.outRecordService = outRecordService;
    }

    @GetMapping("/page")
    public Result<Map<String, Object>> page(@RequestParam(defaultValue = "1") Long pageNum,
                                            @RequestParam(defaultValue = "10") Long pageSize,
                                            @RequestParam(required = false) String keyword,
                                            @RequestParam(required = false) Long materialId,
                                            @RequestParam(required = false) String recordType,
                                            @RequestParam(required = false) String startTime,
                                            @RequestParam(required = false) String endTime) {
        List<StockFlowDTO> all = new ArrayList<>();

        if (recordType == null || "in".equals(recordType)) {
            LambdaQueryWrapper<InRecord> inQw = new LambdaQueryWrapper<>();
            if (keyword != null && !keyword.isEmpty()) {
                inQw.and(w -> w.like(InRecord::getBillNo, keyword).or().like(InRecord::getBatchNo, keyword));
            }
            if (materialId != null) inQw.eq(InRecord::getMaterialId, materialId);
            if (startTime != null && !startTime.isEmpty()) inQw.ge(InRecord::getInTime, startTime);
            if (endTime != null && !endTime.isEmpty()) inQw.le(InRecord::getInTime, endTime);
            for (InRecord r : inRecordService.list(inQw)) {
                StockFlowDTO dto = new StockFlowDTO();
                dto.setRecordType("in");
                dto.setMaterialId(r.getMaterialId());
                dto.setMaterialCode(r.getMaterialCode());
                dto.setMaterialName(r.getMaterialName());
                dto.setBillNo(r.getBillNo());
                dto.setBatchNo(r.getBatchNo());
                dto.setNum(r.getInNum());
                dto.setOperator(r.getInUser());
                dto.setOpTime(r.getInTime());
                all.add(dto);
            }
        }

        if (recordType == null || "out".equals(recordType)) {
            LambdaQueryWrapper<OutRecord> outQw = new LambdaQueryWrapper<>();
            if (keyword != null && !keyword.isEmpty()) {
                outQw.and(w -> w.like(OutRecord::getOutboundCode, keyword).or().like(OutRecord::getBatchNo, keyword));
            }
            if (materialId != null) outQw.eq(OutRecord::getMaterialId, materialId);
            if (startTime != null && !startTime.isEmpty()) outQw.ge(OutRecord::getOutTime, startTime);
            if (endTime != null && !endTime.isEmpty()) outQw.le(OutRecord::getOutTime, endTime);
            for (OutRecord r : outRecordService.list(outQw)) {
                StockFlowDTO dto = new StockFlowDTO();
                dto.setRecordType("out");
                dto.setMaterialId(r.getMaterialId());
                dto.setMaterialCode(r.getMaterialCode());
                dto.setMaterialName(r.getMaterialName());
                dto.setBillNo(r.getOutboundCode());
                dto.setBatchNo(r.getBatchNo());
                dto.setNum(r.getOutNum());
                dto.setOperator(r.getOutUser());
                dto.setOpTime(r.getOutTime());
                all.add(dto);
            }
        }

        all.sort((a, b) -> {
            if (a.getOpTime() == null || b.getOpTime() == null) return 0;
            return b.getOpTime().compareTo(a.getOpTime());
        });

        long total = all.size();
        long from = (pageNum - 1) * pageSize;
        long to = Math.min(from + pageSize, total);
        List<StockFlowDTO> pageData = from < total ? all.subList((int) from, (int) to) : Collections.emptyList();

        Map<String, Object> result = new HashMap<>();
        result.put("records", pageData);
        result.put("total", total);
        return Result.success(result);
    }

    @GetMapping("/export")
    public void export(@RequestParam(required = false) String keyword,
                       @RequestParam(required = false) Long materialId,
                       @RequestParam(required = false) String recordType,
                       @RequestParam(required = false) String startTime,
                       @RequestParam(required = false) String endTime,
                       HttpServletResponse response) throws Exception {
        Map<String, Object> page = page(1L, 100000L, keyword, materialId, recordType, startTime, endTime).getData();
        @SuppressWarnings("unchecked")
        List<StockFlowDTO> list = (List<StockFlowDTO>) page.get("records");
        List<Map<String, Object>> rows = new ArrayList<>();
        for (StockFlowDTO dto : list) {
            Map<String, Object> m = new HashMap<>();
            m.put("recordType", "in".equals(dto.getRecordType()) ? "入库" : "出库");
            m.put("materialCode", dto.getMaterialCode());
            m.put("materialName", dto.getMaterialName());
            m.put("billNo", dto.getBillNo());
            m.put("batchNo", dto.getBatchNo());
            m.put("num", dto.getNum());
            m.put("operator", dto.getOperator());
            m.put("opTime", dto.getOpTime());
            rows.add(m);
        }
        ExcelExportUtil.export(response, "库存流水", new String[]{"操作类型", "物料编码", "物料名称", "单号", "批次号", "数量", "操作人", "时间"},
                rows, new String[]{"recordType", "materialCode", "materialName", "billNo", "batchNo", "num", "operator", "opTime"});
    }
}
