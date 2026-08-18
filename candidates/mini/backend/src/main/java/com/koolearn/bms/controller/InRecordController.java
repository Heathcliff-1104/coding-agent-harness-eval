package com.koolearn.bms.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.koolearn.bms.entity.InRecord;
import com.koolearn.bms.service.InRecordService;
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
@RequestMapping("/inRecord")
public class InRecordController {

    private final InRecordService inRecordService;

    public InRecordController(InRecordService inRecordService) {
        this.inRecordService = inRecordService;
    }

    @GetMapping("/list")
    public Result<List<InRecord>> list(@RequestParam String startDate, @RequestParam String endDate) {
        return Result.success(inRecordService.selectByDate(startDate, endDate));
    }

    @GetMapping("/page")
    public Result<IPage<InRecord>> page(@RequestParam(defaultValue = "1") Long pageNum,
                                        @RequestParam(defaultValue = "10") Long pageSize,
                                        @RequestParam(required = false) String startDate,
                                        @RequestParam(required = false) String endDate,
                                        @RequestParam(required = false) String keyword,
                                        @RequestParam(required = false) String billNo) {
        return Result.success(inRecordService.pageQuery(pageNum, pageSize, startDate, endDate, keyword, billNo));
    }

    @GetMapping("/export")
    public void export(@RequestParam(required = false) String startDate,
                       @RequestParam(required = false) String endDate,
                       @RequestParam(required = false) String keyword,
                       @RequestParam(required = false) String billNo,
                       HttpServletResponse response) throws Exception {
        List<InRecord> data = inRecordService.selectByDate(
                startDate == null ? "1970-01-01" : startDate,
                endDate == null ? LocalDate.now().toString() : endDate);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode("入库记录_" + LocalDate.now() + ".xlsx", "UTF-8"));

        Workbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet("入库记录");
        String[] headers = {"入库单号", "入库时间", "操作人", "物料编码", "物料名称", "批次号", "数量", "存放货位"};
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) headerRow.createCell(i).setCellValue(headers[i]);
        int rowNum = 1;
        for (InRecord r : data) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(r.getBillNo() == null ? "" : r.getBillNo());
            row.createCell(1).setCellValue(r.getInTime() == null ? "" : r.getInTime().toString());
            row.createCell(2).setCellValue(r.getInUser() == null ? "" : r.getInUser());
            row.createCell(3).setCellValue(r.getMaterialCode() == null ? "" : r.getMaterialCode());
            row.createCell(4).setCellValue(r.getMaterialName() == null ? "" : r.getMaterialName());
            row.createCell(5).setCellValue(r.getBatchNo() == null ? "" : r.getBatchNo());
            row.createCell(6).setCellValue(r.getInNum() == null ? "" : r.getInNum().toString());
            row.createCell(7).setCellValue(r.getLocationNo() == null ? "" : r.getLocationNo());
        }
        try (OutputStream os = response.getOutputStream()) {
            wb.write(os);
        }
        wb.close();
    }
}
