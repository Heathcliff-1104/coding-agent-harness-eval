package com.koolearn.bms.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.koolearn.bms.dto.StockFlowDTO;
import com.koolearn.bms.entity.InRecord;
import com.koolearn.bms.entity.Material;
import com.koolearn.bms.entity.OutRecord;
import com.koolearn.bms.mapper.MaterialMapper;
import com.koolearn.bms.service.InRecordService;
import com.koolearn.bms.service.OutRecordService;
import com.koolearn.bms.util.Result;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/stockFlow")
public class StockFlowController {

    private final InRecordService inRecordService;
    private final OutRecordService outRecordService;
    private final MaterialMapper materialMapper;

    public StockFlowController(InRecordService inRecordService, OutRecordService outRecordService,
                               MaterialMapper materialMapper) {
        this.inRecordService = inRecordService;
        this.outRecordService = outRecordService;
        this.materialMapper = materialMapper;
    }

    @GetMapping("/page")
    public Result<Map<String, Object>> page(@RequestParam(defaultValue = "1") Long pageNum,
                                            @RequestParam(defaultValue = "10") Long pageSize,
                                            @RequestParam(required = false) String keyword,
                                            @RequestParam(required = false) Long materialId,
                                            @RequestParam(required = false) String recordType,
                                            @RequestParam(required = false) String startTime,
                                            @RequestParam(required = false) String endTime) {
        List<StockFlowDTO> all = loadAll(keyword, materialId, recordType, startTime, endTime);
        all.sort((a, b) -> {
            if (a.getOpTime() == null) return 1;
            if (b.getOpTime() == null) return -1;
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
        List<StockFlowDTO> data = loadAll(keyword, materialId, recordType, startTime, endTime);
        data.sort((a, b) -> {
            if (a.getOpTime() == null) return 1;
            if (b.getOpTime() == null) return -1;
            return b.getOpTime().compareTo(a.getOpTime());
        });
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode("库存流水_" + LocalDate.now() + ".xlsx", "UTF-8"));

        Workbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet("库存流水");
        String[] headers = {"类型", "单据号", "物料编码", "物料名称", "批次号", "数量", "操作人", "时间"};
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) headerRow.createCell(i).setCellValue(headers[i]);
        int rowNum = 1;
        for (StockFlowDTO dto : data) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(recordTypeLabel(dto.getRecordType()));
            row.createCell(1).setCellValue(dto.getBillNo() == null ? "" : dto.getBillNo());
            row.createCell(2).setCellValue(dto.getMaterialCode() == null ? "" : dto.getMaterialCode());
            row.createCell(3).setCellValue(dto.getMaterialName() == null ? "" : dto.getMaterialName());
            row.createCell(4).setCellValue(dto.getBatchNo() == null ? "" : dto.getBatchNo());
            row.createCell(5).setCellValue(dto.getNum() == null ? "" : dto.getNum().toString());
            row.createCell(6).setCellValue(dto.getOperator() == null ? "" : dto.getOperator());
            row.createCell(7).setCellValue(dto.getOpTime() == null ? "" : dto.getOpTime().toString());
        }
        try (OutputStream os = response.getOutputStream()) {
            wb.write(os);
        }
        wb.close();
    }

    private List<StockFlowDTO> loadAll(String keyword, Long materialId, String recordType,
                                       String startTime, String endTime) {
        List<StockFlowDTO> all = new ArrayList<>();

        if (recordType == null || "in".equals(recordType) || "return".equals(recordType)) {
            LambdaQueryWrapper<InRecord> inQw = new LambdaQueryWrapper<>();
            if (keyword != null && !keyword.isEmpty()) {
                inQw.and(w -> w.like(InRecord::getBillNo, keyword).or().like(InRecord::getBatchNo, keyword));
            }
            if (materialId != null) inQw.eq(InRecord::getMaterialId, materialId);
            if (startTime != null && !startTime.isEmpty()) inQw.ge(InRecord::getInTime, startTime + " 00:00:00");
            if (endTime != null && !endTime.isEmpty()) inQw.le(InRecord::getInTime, endTime + " 23:59:59");
            for (InRecord r : inRecordService.list(inQw)) {
                StockFlowDTO dto = new StockFlowDTO();
                dto.setRecordType("return".equals(recordType) ? "return" : "in");
                dto.setMaterialId(r.getMaterialId());
                dto.setBillNo(r.getBillNo());
                dto.setBatchNo(r.getBatchNo());
                dto.setNum(r.getInNum());
                dto.setOperator(r.getInUser());
                dto.setOpTime(r.getInTime());
                fillMaterial(dto);
                all.add(dto);
            }
        }

        if (recordType == null || "out".equals(recordType)) {
            LambdaQueryWrapper<OutRecord> outQw = new LambdaQueryWrapper<>();
            if (keyword != null && !keyword.isEmpty()) {
                outQw.and(w -> w.like(OutRecord::getOutboundCode, keyword).or().like(OutRecord::getBatchNo, keyword));
            }
            if (materialId != null) outQw.eq(OutRecord::getMaterialId, materialId);
            if (startTime != null && !startTime.isEmpty()) outQw.ge(OutRecord::getOutTime, startTime + " 00:00:00");
            if (endTime != null && !endTime.isEmpty()) outQw.le(OutRecord::getOutTime, endTime + " 23:59:59");
            for (OutRecord r : outRecordService.list(outQw)) {
                StockFlowDTO dto = new StockFlowDTO();
                dto.setRecordType("out");
                dto.setMaterialId(r.getMaterialId());
                dto.setBillNo(r.getOutboundCode());
                dto.setBatchNo(r.getBatchNo());
                dto.setNum(r.getOutNum());
                dto.setOperator(r.getOutUser());
                dto.setOpTime(r.getOutTime());
                fillMaterial(dto);
                all.add(dto);
            }
        }
        return all;
    }

    private void fillMaterial(StockFlowDTO dto) {
        if (dto.getMaterialId() == null) return;
        Material m = materialMapper.selectById(dto.getMaterialId());
        if (m != null) {
            dto.setMaterialCode(m.getMaterialCode());
            dto.setMaterialName(m.getMaterialName());
        }
    }

    private String recordTypeLabel(String type) {
        if ("in".equals(type)) return "入库";
        if ("out".equals(type)) return "出库";
        if ("return".equals(type)) return "退库";
        return type;
    }
}
