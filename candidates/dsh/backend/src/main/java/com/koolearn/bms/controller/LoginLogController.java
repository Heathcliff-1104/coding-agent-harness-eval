package com.koolearn.bms.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.koolearn.bms.annotation.RequireRole;
import com.koolearn.bms.entity.LoginLog;
import com.koolearn.bms.service.LoginLogService;
import com.koolearn.bms.util.ExcelExportUtil;
import com.koolearn.bms.util.Result;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    public void export(@RequestParam(required = false) String username,
                       HttpServletResponse response) throws Exception {
        Page<LoginLog> page = new Page<>(1, 100000);
        LambdaQueryWrapper<LoginLog> qw = new LambdaQueryWrapper<>();
        qw.like(username != null, LoginLog::getUsername, username)
          .orderByDesc(LoginLog::getLoginTime);
        List<LoginLog> list = loginLogService.page(page, qw).getRecords();
        List<Map<String, Object>> rows = new ArrayList<>();
        for (LoginLog l : list) {
            Map<String, Object> m = new HashMap<>();
            m.put("username", l.getUsername());
            m.put("loginIp", l.getLoginIp());
            m.put("deviceInfo", l.getDeviceInfo());
            m.put("loginTime", l.getLoginTime());
            m.put("loginResult", l.getLoginResult() != null && l.getLoginResult() == 1 ? "成功" : "失败");
            rows.add(m);
        }
        ExcelExportUtil.export(response, "登录日志", new String[]{"用户账号", "登陆地址", "设备信息", "登录时间", "登录结果"},
                rows, new String[]{"username", "loginIp", "deviceInfo", "loginTime", "loginResult"});
    }
}
