package com.koolearn.bms.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.koolearn.bms.annotation.RequireRole;
import com.koolearn.bms.entity.LoginLog;
import com.koolearn.bms.service.LoginLogService;
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
@RequestMapping("/loginLog")
public class LoginLogController {

    private final LoginLogService loginLogService;

    public LoginLogController(LoginLogService loginLogService) {
        this.loginLogService = loginLogService;
    }

    @GetMapping("/page")
    public Result<IPage<LoginLog>> page(@RequestParam(defaultValue = "1") Long pageNum,
                                         @RequestParam(defaultValue = "10") Long pageSize,
                                         @RequestParam(required = false) String username) {
        Page<LoginLog> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<LoginLog> qw = new LambdaQueryWrapper<>();
        qw.like(username != null, LoginLog::getUsername, username)
          .orderByDesc(LoginLog::getLoginTime);
        return Result.success(loginLogService.page(page, qw));
    }

    @GetMapping("/export")
    public void export(HttpServletResponse response) throws Exception {
        List<LoginLog> logs = loginLogService.list(new LambdaQueryWrapper<LoginLog>()
                .orderByDesc(LoginLog::getLoginTime));
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode("登录日志_" + LocalDate.now() + ".xlsx", "UTF-8"));
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("登录日志");
            String[] heads = {"用户账号", "登陆地址", "设备信息", "登录时间", "结果"};
            Row hr = sheet.createRow(0);
            for (int i = 0; i < heads.length; i++) hr.createCell(i).setCellValue(heads[i]);
            int n = 1;
            for (LoginLog l : logs) {
                Row row = sheet.createRow(n++);
                row.createCell(0).setCellValue(l.getUsername() == null ? "" : l.getUsername());
                row.createCell(1).setCellValue(l.getLoginIp() == null ? "" : l.getLoginIp());
                row.createCell(2).setCellValue(l.getDeviceInfo() == null ? "" : l.getDeviceInfo());
                row.createCell(3).setCellValue(l.getLoginTime() == null ? "" : l.getLoginTime().toString());
                row.createCell(4).setCellValue(l.getLoginResult() != null && l.getLoginResult() == 1 ? "成功" : "失败");
            }
            wb.write(response.getOutputStream());
        }
    }
}
