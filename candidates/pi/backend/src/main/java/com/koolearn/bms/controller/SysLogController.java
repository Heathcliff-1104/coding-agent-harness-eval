package com.koolearn.bms.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.koolearn.bms.annotation.RequireRole;
import com.koolearn.bms.entity.SysOperationLog;
import com.koolearn.bms.service.SysOperationLogService;
import com.koolearn.bms.util.Result;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.net.URLEncoder;
import java.time.LocalDate;
import java.util.List;

@RequireRole("admin")
@RestController
@RequestMapping("/sysLog")
public class SysLogController {

    private final SysOperationLogService sysOperationLogService;

    public SysLogController(SysOperationLogService sysOperationLogService) {
        this.sysOperationLogService = sysOperationLogService;
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
    public void export(HttpServletResponse response) throws Exception {
        List<SysOperationLog> logs = sysOperationLogService.list(new LambdaQueryWrapper<SysOperationLog>()
                .orderByDesc(SysOperationLog::getCreateTime));
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode("操作日志_" + LocalDate.now() + ".xlsx", "UTF-8"));
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("操作日志");
            String[] heads = {"操作人", "操作", "描述", "IP", "结果", "时间"};
            Row hr = sheet.createRow(0);
            for (int i = 0; i < heads.length; i++) hr.createCell(i).setCellValue(heads[i]);
            int n = 1;
            for (SysOperationLog l : logs) {
                Row row = sheet.createRow(n++);
                row.createCell(0).setCellValue(l.getUsername() == null ? "" : l.getUsername());
                row.createCell(1).setCellValue(l.getOperation() == null ? "" : l.getOperation());
                row.createCell(2).setCellValue(l.getDescription() == null ? "" : l.getDescription());
                row.createCell(3).setCellValue(l.getIp() == null ? "" : l.getIp());
                row.createCell(4).setCellValue(l.getResult() == null ? "" : l.getResult());
                row.createCell(5).setCellValue(l.getCreateTime() == null ? "" : l.getCreateTime().toString());
            }
            wb.write(response.getOutputStream());
        }
    }
}
