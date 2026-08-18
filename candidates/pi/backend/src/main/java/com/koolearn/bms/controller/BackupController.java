package com.koolearn.bms.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.koolearn.bms.annotation.RequireRole;
import com.koolearn.bms.entity.BackupConfig;
import com.koolearn.bms.entity.BackupRecord;
import com.koolearn.bms.mapper.BackupConfigMapper;
import com.koolearn.bms.mapper.BackupRecordMapper;
import com.koolearn.bms.service.BackupService;
import com.koolearn.bms.service.SysOperationLogService;
import com.koolearn.bms.util.Result;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RequireRole("admin")
@RestController
@RequestMapping("/backup")
public class BackupController {

    private final BackupService backupService;
    private final BackupConfigMapper backupConfigMapper;
    private final BackupRecordMapper backupRecordMapper;
    private final SysOperationLogService sysLogService;

    public BackupController(BackupService backupService,
                            BackupConfigMapper backupConfigMapper,
                            BackupRecordMapper backupRecordMapper,
                            SysOperationLogService sysLogService) {
        this.backupService = backupService;
        this.backupConfigMapper = backupConfigMapper;
        this.backupRecordMapper = backupRecordMapper;
        this.sysLogService = sysLogService;
    }

    @PostMapping("/db")
    public Result<String> backup(@RequestAttribute("username") String operator,
                                 HttpServletRequest request) {
        String msg = backupService.backup("full");
        sysLogService.log(operator, "数据备份", "手动全量备份: " + msg, getIp(request));
        return msg.startsWith("备份成功") ? Result.success(msg) : Result.fail(msg);
    }

    @GetMapping("/record/page")
    public Result<IPage<BackupRecord>> recordPage(@RequestParam(defaultValue = "1") Long pageNum,
                                                  @RequestParam(defaultValue = "10") Long pageSize) {
        Page<BackupRecord> page = new Page<>(pageNum, pageSize);
        return Result.success(backupRecordMapper.selectPage(page,
                new LambdaQueryWrapper<BackupRecord>().orderByDesc(BackupRecord::getCreateTime)));
    }

    @GetMapping("/config")
    public Result<List<BackupConfig>> config() {
        return Result.success(backupConfigMapper.selectList(null));
    }

    @PutMapping("/config")
    public Result<String> saveConfig(@RequestBody List<BackupConfig> configs,
                                     @RequestAttribute("username") String operator,
                                     HttpServletRequest request) {
        if (configs != null) {
            for (BackupConfig cfg : configs) {
                if (cfg.getId() == null) {
                    backupConfigMapper.insert(cfg);
                } else {
                    backupConfigMapper.updateById(cfg);
                }
            }
        }
        sysLogService.log(operator, "修改备份配置", "更新自动备份策略", getIp(request));
        return Result.success("配置已保存");
    }

    private String getIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) ip = request.getHeader("X-Real-IP");
        if (ip == null || ip.isEmpty()) ip = request.getRemoteAddr();
        return ip;
    }
}
