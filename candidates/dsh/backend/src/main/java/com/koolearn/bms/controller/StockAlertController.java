package com.koolearn.bms.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.koolearn.bms.annotation.RequireRole;
import com.koolearn.bms.entity.Material;
import com.koolearn.bms.entity.PurchaseRequest;
import com.koolearn.bms.entity.StockAlert;
import com.koolearn.bms.mapper.PurchaseRequestMapper;
import com.koolearn.bms.service.MaterialService;
import com.koolearn.bms.service.StockAlertService;
import com.koolearn.bms.util.Result;
import com.koolearn.bms.util.dingtalk.DingTalkNotifier;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/stockAlert")
public class StockAlertController {

    private final StockAlertService stockAlertService;
    private final MaterialService materialService;
    private final PurchaseRequestMapper purchaseRequestMapper;
    private final DingTalkNotifier dingTalkNotifier;

    public StockAlertController(StockAlertService stockAlertService, MaterialService materialService,
                                PurchaseRequestMapper purchaseRequestMapper, DingTalkNotifier dingTalkNotifier) {
        this.stockAlertService = stockAlertService;
        this.materialService = materialService;
        this.purchaseRequestMapper = purchaseRequestMapper;
        this.dingTalkNotifier = dingTalkNotifier;
    }

    /** 补货申请（需求 2.4.2）：记录采购补货需求并通知采购员 */
    @RequireRole({"admin", "warehouse"})
    @PostMapping("/purchaseRequest")
    public Result<String> purchaseRequest(@RequestBody Map<String, Object> body,
                                          @RequestAttribute("username") String operator) {
        Long materialId = body.get("materialId") == null ? null : Long.valueOf(body.get("materialId").toString());
        String quantityStr = body.get("quantity") == null ? "0" : body.get("quantity").toString();
        java.math.BigDecimal quantity;
        try {
            quantity = new java.math.BigDecimal(quantityStr);
        } catch (NumberFormatException e) {
            return Result.fail("采购数量格式错误");
        }
        if (quantity.compareTo(java.math.BigDecimal.ZERO) <= 0) {
            return Result.fail("采购数量必须大于0");
        }
        Material mat = materialId != null ? materialService.getById(materialId) : null;
        PurchaseRequest req = new PurchaseRequest();
        req.setMaterialId(materialId);
        req.setMaterialCode(mat != null ? mat.getMaterialCode() : null);
        req.setMaterialName(mat != null ? mat.getMaterialName() : null);
        req.setManufacturer(mat != null ? mat.getManufacturer() : null);
        req.setQuantity(quantity);
        req.setRemark(body.get("remark") == null ? null : body.get("remark").toString());
        req.setStatus("pending");
        req.setCreateBy(operator);
        purchaseRequestMapper.insert(req);
        String content = "物料【" + (mat != null ? mat.getMaterialName() : materialId) + "】需要补货 "
                + quantity + "，厂家:" + (mat != null && mat.getManufacturer() != null ? mat.getManufacturer() : "待确认")
                + "，请采购员跟进。";
        dingTalkNotifier.sendText("补货申请", content);
        return Result.success("补货申请已提交并通知采购员");
    }

    @RequireRole({"admin", "warehouse", "purchaser", "manager"})
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
