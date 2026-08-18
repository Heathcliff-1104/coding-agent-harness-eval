package com.koolearn.bms.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.koolearn.bms.entity.OutRecord;
import com.koolearn.bms.mapper.OutRecordMapper;
import com.koolearn.bms.service.OutRecordService;
import com.koolearn.bms.util.Result;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletResponse;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/outRecord")
public class OutRecordController {

    private final OutRecordService outRecordService;
    private final OutRecordMapper outRecordMapper;

    public OutRecordController(OutRecordService outRecordService, OutRecordMapper outRecordMapper) {
        this.outRecordService = outRecordService;
        this.outRecordMapper = outRecordMapper;
    }

    @GetMapping("/page")
    public Result<IPage<OutRecord>> pageList(
            @RequestParam(defaultValue = "1") Long pageNum,
            @RequestParam(defaultValue = "10") Long pageSize,
            @RequestParam(required = false) String outboundCode,
            @RequestParam(required = false) Long materialId,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(required = false) String keyword) {
        Page<OutRecord> page = new Page<>(pageNum, pageSize);
        return Result.success(outRecordMapper.selectPageByCondition(page, outboundCode, materialId, startTime, endTime, keyword));
    }

    @GetMapping("/export")
    public void export(@RequestParam(required = false) String outboundCode,
                       @RequestParam(required = false) String startTime,
                       @RequestParam(required = false) String endTime,
                       HttpServletResponse response) throws Exception {
        List<OutRecord> data = outRecordMapper.selectPageByCondition(
                new Page<>(1, 100000), outboundCode, null,
                startTime == null ? "1970-01-01" : startTime,
                endTime == null ? LocalDate.now().toString() : endTime, null).getRecords();
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode("出库记录_" + LocalDate.now() + ".xlsx", "UTF-8"));

        Workbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet("出库记录");
        String[] headers = {"出库单号", "物料编码", "物料名称", "批次号", "出库数量", "操作人", "出库时间"};
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) headerRow.createCell(i).setCellValue(headers[i]);
        int rowNum = 1;
        for (OutRecord r : data) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(r.getOutboundCode() == null ? "" : r.getOutboundCode());
            row.createCell(1).setCellValue(r.getMaterialCode() == null ? "" : r.getMaterialCode());
            row.createCell(2).setCellValue(r.getMaterialName() == null ? "" : r.getMaterialName());
            row.createCell(3).setCellValue(r.getBatchNo() == null ? "" : r.getBatchNo());
            row.createCell(4).setCellValue(r.getOutNum() == null ? "" : r.getOutNum().toString());
            row.createCell(5).setCellValue(r.getOutUser() == null ? "" : r.getOutUser());
            row.createCell(6).setCellValue(r.getOutTime() == null ? "" : r.getOutTime().toString());
        }
        try (OutputStream os = response.getOutputStream()) {
            wb.write(os);
        }
        wb.close();
    }
}

