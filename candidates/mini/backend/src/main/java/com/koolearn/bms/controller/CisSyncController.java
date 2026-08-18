package com.koolearn.bms.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.koolearn.bms.annotation.RequireRole;
import com.koolearn.bms.entity.SysCisSyncLog;
import com.koolearn.bms.service.CisSyncService;
import com.koolearn.bms.service.SysCisSyncLogService;
import com.koolearn.bms.util.Result;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cis")
public class CisSyncController {

    private final CisSyncService cisSyncService;
    private final SysCisSyncLogService syncLogService;

    public CisSyncController(CisSyncService cisSyncService, SysCisSyncLogService syncLogService) {
        this.cisSyncService = cisSyncService;
        this.syncLogService = syncLogService;
    }

    @RequireRole({"admin", "warehouse"})
    @PostMapping("/sync")
    public Result<String> sync(@RequestParam(defaultValue = "full") String type) {
        return Result.success(cisSyncService.sync(type));
    }

    @GetMapping("/logs")
    public Result<IPage<SysCisSyncLog>> logs(@RequestParam(defaultValue = "1") Long pageNum,
                                             @RequestParam(defaultValue = "10") Long pageSize) {
        Page<SysCisSyncLog> page = new Page<>(pageNum, pageSize);
        return Result.success(syncLogService.page(page,
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysCisSyncLog>()
                        .orderByDesc(SysCisSyncLog::getCreateTime)));
    }
}
