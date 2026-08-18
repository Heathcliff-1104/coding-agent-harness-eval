package com.koolearn.bms.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.koolearn.bms.annotation.RequireRole;
import com.koolearn.bms.entity.LoginLog;
import com.koolearn.bms.service.LoginLogService;
import com.koolearn.bms.util.Result;
import org.springframework.web.bind.annotation.*;

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
}
