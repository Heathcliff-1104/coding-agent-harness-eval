package com.koolearn.bms.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.koolearn.bms.annotation.RequireRole;
import com.koolearn.bms.entity.SysOperationLog;
import com.koolearn.bms.service.SysOperationLogService;
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
    public void export(@RequestParam(required = false) String username,
                       @RequestParam(required = false) String operation,
                       HttpServletResponse response) throws Exception {
        Page<SysOperationLog> page = new Page<>(1, 100000);
        LambdaQueryWrapper<SysOperationLog> qw = new LambdaQueryWrapper<>();
        qw.like(username != null, SysOperationLog::getUsername, username)
          .like(operation != null, SysOperationLog::getOperation, operation)
          .orderByDesc(SysOperationLog::getCreateTime);
        List<SysOperationLog> list = sysOperationLogService.page(page, qw).getRecords();
        List<Map<String, Object>> rows = new ArrayList<>();
        for (SysOperationLog l : list) {
            Map<String, Object> m = new HashMap<>();
            m.put("username", l.getUsername());
            m.put("operation", l.getOperation());
            m.put("description", l.getDescription());
            m.put("ip", l.getIp());
            m.put("result", l.getResult());
            m.put("createTime", l.getCreateTime());
            rows.add(m);
        }
        ExcelExportUtil.export(response, "系统日志", new String[]{"操作人", "操作类型", "操作描述", "IP地址", "操作结果", "操作时间"},
                rows, new String[]{"username", "operation", "description", "ip", "result", "createTime"});
    }
}
