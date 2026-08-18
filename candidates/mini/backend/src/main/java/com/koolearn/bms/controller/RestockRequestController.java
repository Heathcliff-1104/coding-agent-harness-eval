package com.koolearn.bms.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.koolearn.bms.annotation.RequireRole;
import com.koolearn.bms.entity.SysRestockRequest;
import com.koolearn.bms.service.SysRestockRequestService;
import com.koolearn.bms.util.Result;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/restock")
public class RestockRequestController {

    private final SysRestockRequestService restockRequestService;

    public RestockRequestController(SysRestockRequestService restockRequestService) {
        this.restockRequestService = restockRequestService;
    }

    @GetMapping("/page")
    public Result<IPage<SysRestockRequest>> page(@RequestParam(defaultValue = "1") Long pageNum,
                                                 @RequestParam(defaultValue = "10") Long pageSize,
                                                 @RequestParam(required = false) Integer status) {
        Page<SysRestockRequest> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SysRestockRequest> qw = new LambdaQueryWrapper<>();
        qw.eq(status != null, SysRestockRequest::getStatus, status)
          .orderByDesc(SysRestockRequest::getCreateTime);
        return Result.success(restockRequestService.page(page, qw));
    }

    @RequireRole({"admin", "warehouse", "purchaser"})
    @PostMapping("/create")
    public Result<String> create(@RequestBody SysRestockRequest req, @RequestAttribute("username") String username) {
        if (req.getMaterialId() == null) return Result.fail("物料不能为空");
        if (req.getPurchaseQty() == null || req.getPurchaseQty().compareTo(BigDecimal.ZERO) <= 0) {
            return Result.fail("采购数量必须大于0");
        }
        req.setRequester(username);
        req.setStatus(0);
        req.setCreateTime(LocalDateTime.now());
        restockRequestService.save(req);
        return Result.success("补货申请已提交");
    }

    @RequireRole({"admin", "warehouse"})
    @PostMapping("/handle/{id}")
    public Result<String> handle(@PathVariable Long id, @RequestParam String method) {
        SysRestockRequest req = restockRequestService.getById(id);
        if (req == null) return Result.fail("补货申请不存在");
        req.setStatus(1);
        restockRequestService.updateById(req);
        return Result.success("已标记为已采购: " + method);
    }
}
