package com.koolearn.bms.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.koolearn.bms.annotation.RequirePermission;
import com.koolearn.bms.annotation.RequireRole;
import com.koolearn.bms.entity.CisSyncLog;
import com.koolearn.bms.service.CisSyncService;
import com.koolearn.bms.service.SysOperationLogService;
import com.koolearn.bms.util.Result;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

/**
 * 同步CIS元件库（需求 2.4.3）：手动触发全量/增量同步 + 同步日志查询。
 * 演示模式（未配置 cis.endpoint 或钉钉 mock 开启）下为模拟同步，不调用外部系统。
 */
@RequireRole({"admin", "warehouse"})
@RestController
@RequestMapping("/cis")
public class CisSyncController {

    private final CisSyncService cisSyncService;
    private final SysOperationLogService sysLogService;

    public CisSyncController(CisSyncService cisSyncService, SysOperationLogService sysLogService) {
        this.cisSyncService = cisSyncService;
        this.sysLogService = sysLogService;
    }

    @RequirePermission("btn:cis:sync")
    @PostMapping("/sync/full")
    public Result<String> syncFull(@RequestAttribute("username") String operator, HttpServletRequest request) {
        String result = cisSyncService.syncFull();
        sysLogService.log(operator, "同步CIS", "手动全量同步CIS: " + result, getIp(request));
        return Result.success(result);
    }

    @RequirePermission("btn:cis:sync")
    @PostMapping("/sync/incremental")
    public Result<String> syncIncremental(@RequestAttribute("username") String operator, HttpServletRequest request) {
        String result = cisSyncService.syncIncremental();
        sysLogService.log(operator, "同步CIS", "手动增量同步CIS: " + result, getIp(request));
        return Result.success(result);
    }

    @GetMapping("/sync/log")
    public Result<IPage<CisSyncLog>> pageLogs(@RequestParam(defaultValue = "1") Long pageNum,
                                              @RequestParam(defaultValue = "10") Long pageSize) {
        return Result.success(cisSyncService.pageLogs(pageNum, pageSize));
    }

    private String getIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) ip = request.getHeader("X-Real-IP");
        if (ip == null || ip.isEmpty()) ip = request.getRemoteAddr();
        return ip;
    }
}
