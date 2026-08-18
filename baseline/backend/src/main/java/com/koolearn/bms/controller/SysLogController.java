package com.koolearn.bms.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.koolearn.bms.annotation.RequireRole;
import com.koolearn.bms.entity.SysOperationLog;
import com.koolearn.bms.service.SysOperationLogService;
import com.koolearn.bms.util.Result;
import org.springframework.web.bind.annotation.*;

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
}
