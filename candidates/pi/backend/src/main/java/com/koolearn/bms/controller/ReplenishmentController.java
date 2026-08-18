package com.koolearn.bms.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.koolearn.bms.annotation.RequireRole;
import com.koolearn.bms.entity.Replenishment;
import com.koolearn.bms.mapper.ReplenishmentMapper;
import com.koolearn.bms.service.DingTalkNotifier;
import com.koolearn.bms.service.SysOperationLogService;
import com.koolearn.bms.util.Result;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Arrays;

@RestController
@RequestMapping("/replenishment")
public class ReplenishmentController {

    private final ReplenishmentMapper replenishmentMapper;
    private final DingTalkNotifier notifier;
    private final SysOperationLogService sysLogService;

    public ReplenishmentController(ReplenishmentMapper replenishmentMapper,
                                   DingTalkNotifier notifier,
                                   SysOperationLogService sysLogService) {
        this.replenishmentMapper = replenishmentMapper;
        this.notifier = notifier;
        this.sysLogService = sysLogService;
    }

    /**
     * 补货申请：库管员/管理员/采购员均可发起（需求：采购员填写厂家联系方式+采购数量）。
     */
    @RequireRole({"admin", "warehouse", "purchaser"})
    @PostMapping("/apply")
    public Result<String> apply(@RequestBody Replenishment req,
                                @RequestAttribute("username") String operator,
                                @RequestAttribute(name = "realName", required = false) String realName,
                                HttpServletRequest request) {
        if (req.getMaterialId() == null || req.getShortage() == null) {
            return Result.fail("物料与缺货数量必填");
        }
        req.setId(null);
        req.setApplicant(realName != null && !realName.isEmpty() ? realName : operator);
        req.setStatus(0);
        req.setCreateTime(LocalDateTime.now());
        replenishmentMapper.insert(req);
        sysLogService.log(operator, "补货申请", "补货申请: " + req.getMaterialName() + " 缺货:" + req.getShortage(), getIp(request));
        // 通知采购员
        notifier.send("补货申请", "物料【" + req.getMaterialName() + "】缺货 " + req.getShortage()
                + "，申请采购数量 " + req.getPurchaseNum() + "，厂家联系方式: " + (req.getSupplierContact() == null ? "" : req.getSupplierContact()),
                Arrays.asList("采购员"));
        return Result.success("补货申请已提交");
    }

    @RequireRole({"admin", "warehouse"})
    @GetMapping("/page")
    public Result<IPage<Replenishment>> page(@RequestParam(defaultValue = "1") Long pageNum,
                                             @RequestParam(defaultValue = "10") Long pageSize,
                                             @RequestParam(required = false) Integer status) {
        Page<Replenishment> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Replenishment> qw = new LambdaQueryWrapper<>();
        qw.eq(status != null, Replenishment::getStatus, status)
          .orderByDesc(Replenishment::getCreateTime);
        return Result.success(replenishmentMapper.selectPage(page, qw));
    }

    @RequireRole({"admin", "warehouse"})
    @PostMapping("/handle/{id}")
    public Result<String> handle(@PathVariable Long id,
                                 @RequestAttribute("username") String operator,
                                 HttpServletRequest request) {
        Replenishment rep = replenishmentMapper.selectById(id);
        if (rep == null) return Result.fail("补货申请不存在");
        rep.setStatus(1);
        replenishmentMapper.updateById(rep);
        sysLogService.log(operator, "补货处理", "补货申请已处理: " + rep.getMaterialName(), getIp(request));
        return Result.success("已标记为处理完成");
    }

    private String getIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) ip = request.getHeader("X-Real-IP");
        if (ip == null || ip.isEmpty()) ip = request.getRemoteAddr();
        return ip;
    }
}
