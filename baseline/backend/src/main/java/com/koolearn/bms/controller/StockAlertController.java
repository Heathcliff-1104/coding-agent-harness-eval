package com.koolearn.bms.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.koolearn.bms.annotation.RequireRole;
import com.koolearn.bms.entity.StockAlert;
import com.koolearn.bms.service.StockAlertService;
import com.koolearn.bms.util.Result;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/stockAlert")
public class StockAlertController {

    private final StockAlertService stockAlertService;

    public StockAlertController(StockAlertService stockAlertService) {
        this.stockAlertService = stockAlertService;
    }

    @GetMapping("/page")
    public Result<IPage<StockAlert>> page(@RequestParam(defaultValue = "1") Long pageNum,
                                          @RequestParam(defaultValue = "10") Long pageSize,
                                          @RequestParam(required = false) Integer alertType,
                                          @RequestParam(required = false) Integer handled) {
        Page<StockAlert> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<StockAlert> qw = new LambdaQueryWrapper<>();
        qw.eq(alertType != null, StockAlert::getAlertType, alertType)
          .eq(handled != null, StockAlert::getHandled, handled)
          .orderByDesc(StockAlert::getCreateTime);
        return Result.success(stockAlertService.page(page, qw));
    }

    @RequireRole({"admin", "warehouse"})
    @PostMapping("/handle/{id}")
    public Result<String> handle(@PathVariable Long id, @RequestParam String handler, @RequestParam String method) {
        StockAlert alert = stockAlertService.getById(id);
        if (alert == null) return Result.fail("预警记录不存在");
        alert.setHandled(1);
        alert.setHandler(handler);
        alert.setHandleMethod(method);
        alert.setHandleTime(LocalDateTime.now());
        stockAlertService.updateById(alert);
        return Result.success("处理成功");
    }

    @RequireRole("admin")
    @PostMapping("/scan")
    public Result<String> manualScan() {
        stockAlertService.scanAndAlert();
        return Result.success("手动扫描完成");
    }
}
