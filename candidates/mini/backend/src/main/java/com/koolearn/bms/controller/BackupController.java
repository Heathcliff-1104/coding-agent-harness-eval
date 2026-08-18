package com.koolearn.bms.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.koolearn.bms.annotation.RequireRole;
import com.koolearn.bms.entity.SysBackupRecord;
import com.koolearn.bms.service.BackupService;
import com.koolearn.bms.service.SysConfigService;
import com.koolearn.bms.util.Result;
import org.springframework.web.bind.annotation.*;

@RequireRole("admin")
@RestController
@RequestMapping("/backup")
public class BackupController {

    private final BackupService backupService;
    private final SysConfigService sysConfigService;

    public BackupController(BackupService backupService, SysConfigService sysConfigService) {
        this.backupService = backupService;
        this.sysConfigService = sysConfigService;
    }

    @PostMapping("/db")
    public Result<String> backup() {
        return Result.success(backupService.backup("full"));
    }

    @GetMapping("/records")
    public Result<IPage<SysBackupRecord>> records(@RequestParam(defaultValue = "1") Long pageNum,
                                                  @RequestParam(defaultValue = "10") Long pageSize) {
        return Result.success(backupService.listRecords(pageNum, pageSize));
    }

    @GetMapping("/strategy")
    public Result<java.util.Map<String, String>> getStrategy() {
        java.util.Map<String, String> map = new java.util.HashMap<>();
        map.put("retentionDays", sysConfigService.get("backup.retention.days", "30"));
        map.put("fullCron", sysConfigService.get("backup.full.cron", "0 0 2 * * SUN"));
        map.put("incrementalEnabled", sysConfigService.get("backup.incremental.enabled", "true"));
        return Result.success(map);
    }

    @PostMapping("/strategy")
    public Result<String> setStrategy(@RequestParam(required = false) Integer retentionDays,
                                      @RequestParam(required = false) String fullCron,
                                      @RequestParam(required = false) Boolean incrementalEnabled) {
        if (retentionDays != null) {
            sysConfigService.set("backup.retention.days", String.valueOf(retentionDays), "备份保留天数");
        }
        if (fullCron != null && !fullCron.trim().isEmpty()) {
            sysConfigService.set("backup.full.cron", fullCron.trim(), "全量备份 cron");
        }
        if (incrementalEnabled != null) {
            sysConfigService.set("backup.incremental.enabled", String.valueOf(incrementalEnabled), "是否启用增量备份");
        }
        return Result.success("备份策略已保存");
    }
}
