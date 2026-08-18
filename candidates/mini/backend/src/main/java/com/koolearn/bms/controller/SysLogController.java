package com.koolearn.bms.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.koolearn.bms.annotation.RequireRole;
import com.koolearn.bms.entity.SysOperationLog;
import com.koolearn.bms.service.SysConfigService;
import com.koolearn.bms.service.SysOperationLogService;
import com.koolearn.bms.util.Result;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.time.LocalDate;
import java.util.List;

@RequireRole("admin")
@RestController
@RequestMapping("/sysLog")
public class SysLogController {

    private final SysOperationLogService sysOperationLogService;
    private final SysConfigService sysConfigService;

    public SysLogController(SysOperationLogService sysOperationLogService,
                            SysConfigService sysConfigService) {
        this.sysOperationLogService = sysOperationLogService;
        this.sysConfigService = sysConfigService;
    }

    @GetMapping("/page")
    public Result<IPage<SysOperationLog>> page(@RequestParam(defaultValue = "1") Long pageNum,
                                                @RequestParam(defaultValue = "10") Long pageSize,
                                                @RequestParam(required = false) String username,
                                                @RequestParam(required = false) String operation) {
        Page<SysOperationLog> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SysOperationLog> qw = new LambdaQueryWrapper<>();
        qw.like(username != null, SysOperationLog::getUsername, username)
          .like(operation != null, SysOperationLog::getOperation, operation)
          .orderByDesc(SysOperationLog::getCreateTime);
        return Result.success(sysOperationLogService.page(page, qw));
    }

    @GetMapping("/export")
    public void export(@RequestParam(required = false) String username,
                       @RequestParam(required = false) String operation,
                       HttpServletResponse response) throws Exception {
        LambdaQueryWrapper<SysOperationLog> qw = new LambdaQueryWrapper<>();
        qw.like(username != null, SysOperationLog::getUsername, username)
          .like(operation != null, SysOperationLog::getOperation, operation)
          .orderByDesc(SysOperationLog::getCreateTime);
        List<SysOperationLog> data = sysOperationLogService.list(qw);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode("系统日志_" + LocalDate.now() + ".xlsx", "UTF-8"));
        Workbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet("系统日志");
        String[] headers = {"操作人", "操作类型", "操作描述", "IP地址", "结果", "操作时间"};
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) headerRow.createCell(i).setCellValue(headers[i]);
        int rowNum = 1;
        for (SysOperationLog log : data) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(log.getUsername() == null ? "" : log.getUsername());
            row.createCell(1).setCellValue(log.getOperation() == null ? "" : log.getOperation());
            row.createCell(2).setCellValue(log.getDescription() == null ? "" : log.getDescription());
            row.createCell(3).setCellValue(log.getIp() == null ? "" : log.getIp());
            row.createCell(4).setCellValue(log.getResult() == null ? "" : log.getResult());
            row.createCell(5).setCellValue(log.getCreateTime() == null ? "" : log.getCreateTime().toString());
        }
        try (OutputStream os = response.getOutputStream()) {
            wb.write(os);
        }
        wb.close();
    }

    @GetMapping("/retention")
    public Result<String> getRetention() {
        return Result.success(sysConfigService.get("log.retention.days", "365"));
    }

    @PostMapping("/retention")
    public Result<String> setRetention(@RequestParam Integer days) {
        if (days == null || days < 1 || days > 3650) return Result.fail("保留天数须在1~3650之间");
        sysConfigService.set("log.retention.days", String.valueOf(days), "系统日志保留天数");
        return Result.success("已设置日志保留天数: " + days);
    }
}
