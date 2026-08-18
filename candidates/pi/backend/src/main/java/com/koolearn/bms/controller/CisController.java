package com.koolearn.bms.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.koolearn.bms.annotation.RequireRole;
import com.koolearn.bms.entity.CisSyncLog;
import com.koolearn.bms.mapper.CisSyncLogMapper;
import com.koolearn.bms.service.CisSyncService;
import com.koolearn.bms.util.Result;
import org.springframework.web.bind.annotation.*;

@RequireRole("admin")
@RestController
@RequestMapping("/cis")
public class CisController {

    private final CisSyncService cisSyncService;
    private final CisSyncLogMapper cisSyncLogMapper;

    public CisController(CisSyncService cisSyncService, CisSyncLogMapper cisSyncLogMapper) {
        this.cisSyncService = cisSyncService;
        this.cisSyncLogMapper = cisSyncLogMapper;
    }

    @PostMapping("/sync/full")
    public Result<String> syncFull() {
        cisSyncService.syncFull();
        return Result.success("全量同步已触发");
    }

    @PostMapping("/sync/incremental")
    public Result<String> syncIncremental() {
        cisSyncService.syncIncremental();
        return Result.success("增量同步已触发");
    }

    @GetMapping("/sync/log/page")
    public Result<IPage<CisSyncLog>> logPage(@RequestParam(defaultValue = "1") Long pageNum,
                                             @RequestParam(defaultValue = "10") Long pageSize) {
        Page<CisSyncLog> page = new Page<>(pageNum, pageSize);
        return Result.success(cisSyncLogMapper.selectPage(page,
                new LambdaQueryWrapper<CisSyncLog>().orderByDesc(CisSyncLog::getSyncTime)));
    }
}
