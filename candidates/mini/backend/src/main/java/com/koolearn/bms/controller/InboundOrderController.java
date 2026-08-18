package com.koolearn.bms.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.koolearn.bms.annotation.RequireRole;
import com.koolearn.bms.dto.InboundOrderDTO;
import com.koolearn.bms.entity.InStorageItem;
import com.koolearn.bms.entity.InboundOrder;
import com.koolearn.bms.service.InboundOrderService;
import com.koolearn.bms.service.InStorageItemService;
import com.koolearn.bms.util.Result;
import com.koolearn.bms.util.dingtalk.DingTalkCallbackVerifier;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/inbound")
public class InboundOrderController {

    private final InboundOrderService inboundOrderService;
    private final InStorageItemService inStorageItemService;
    private final DingTalkCallbackVerifier callbackVerifier;

    public InboundOrderController(InboundOrderService inboundOrderService,
                                   InStorageItemService inStorageItemService,
                                   DingTalkCallbackVerifier callbackVerifier) {
        this.inboundOrderService = inboundOrderService;
        this.inStorageItemService = inStorageItemService;
        this.callbackVerifier = callbackVerifier;
    }

    @RequireRole({"admin", "warehouse", "engineer", "purchaser"})
    @PostMapping("/saveDraft")
    public Result<Void> saveDraft(@RequestBody InboundOrderDTO dto) {
        inboundOrderService.saveDraft(dto);
        return Result.success();
    }

    @RequireRole({"admin", "warehouse"})
    @PostMapping("/confirm/{id}")
    public Result<Void> confirmIn(@PathVariable Long id, @RequestParam String operUser) {
        inboundOrderService.confirmIn(id, operUser);
        return Result.success();
    }

    @RequireRole({"admin", "warehouse", "engineer", "purchaser"})
    @PutMapping("/editDraft/{id}")
    public Result<Void> editDraft(@PathVariable Long id, @RequestBody InboundOrderDTO dto) {
        inboundOrderService.editDraft(dto, id);
        return Result.success();
    }

    @GetMapping("/ding/{instanceId}")
    public Result<InboundOrder> getByDing(@PathVariable String instanceId) {
        return Result.success(inboundOrderService.getByDingInstanceId(instanceId));
    }

    @RequireRole({"admin", "warehouse"})
    @PostMapping("/updateStatus")
    public Result<Boolean> updateStatus(@RequestParam Long id, @RequestParam Integer status) {
        return Result.success(inboundOrderService.updateStatus(id, status));
    }

    @GetMapping("/page")
    public Result<IPage<InboundOrder>> getOrderPage(
            @RequestParam(defaultValue = "1") Long pageNum,
            @RequestParam(defaultValue = "10") Long pageSize,
            @RequestParam(required = false) String billNo,
            @RequestParam(required = false) String supplier,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword) {
        return Result.success(inboundOrderService.getOrderPage(pageNum, pageSize, billNo, supplier, status, keyword));
    }

    @GetMapping("/get/{id}")
    public Result<InboundOrderDTO> getDetail(@PathVariable Long id) {
        return Result.success(inboundOrderService.getDetailById(id));
    }

    @RequireRole({"admin", "warehouse", "engineer", "purchaser"})
    @PostMapping("/saveOrder")
    public Result<String> saveOrder(@RequestBody InboundOrderDTO dto) {
        return Result.success(inboundOrderService.saveOrder(dto));
    }

    @PostMapping("/dingtalk/callback")
    public Result<String> dingTalkCallback(@RequestBody Map<String, Object> params,
                                           HttpServletRequest request) {
        String instanceId = (String) params.get("instanceId");
        String result = (String) params.get("result");
        String timestamp = (String) params.get("timestamp");
        String signature = request.getHeader("X-DingTalk-Signature");
        if (!callbackVerifier.verify(instanceId, result, timestamp, signature)) {
            return Result.fail(403, "回调签名校验失败");
        }
        if (!StringUtils.hasText(instanceId)) {
            return Result.fail("参数异常");
        }

        InboundOrder order = inboundOrderService.getByDingInstanceId(instanceId);
        if (order == null) {
            return Result.fail("单据不存在");
        }
        if (order.getOrderStatus() == 1 || order.getOrderStatus() == 2) {
            return Result.success("处理完成");
        }

        if ("agree".equals(result)) {
            inboundOrderService.updateStatus(order.getId(), 0);
        } else if ("refuse".equals(result)) {
            inboundOrderService.updateStatus(order.getId(), 2);
        }
        return Result.success("回调处理成功");
    }

    @RequireRole({"admin", "warehouse"})
    @PostMapping("/confirmBatch")
    public Result<Void> confirmBatch(@RequestBody List<Long> ids, @RequestParam String operUser) {
        if (ids == null || ids.isEmpty()) {
            return Result.fail("请选择需要确认的入库单");
        }
        List<Long> ok = new ArrayList<>();
        List<String> failed = new ArrayList<>();
        for (Long id : ids) {
            try {
                inboundOrderService.confirmIn(id, operUser);
                ok.add(id);
            } catch (Exception e) {
                failed.add(id + ":" + e.getMessage());
            }
        }
        if (failed.isEmpty()) {
            return Result.success();
        }
        return Result.fail("部分确认失败: " + String.join("; ", failed));
    }

    @GetMapping("/export/{id}")
    public void exportOrder(@PathVariable Long id, HttpServletResponse response) throws Exception {
        InboundOrder order = inboundOrderService.getById(id);
        if (order == null) throw new RuntimeException("单据不存在");
        List<InStorageItem> items = inStorageItemService.selectByInboundId(id);

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode("入库单_" + order.getBillNo() + ".xlsx", "UTF-8"));

        Workbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet("入库单");
        sheet.createRow(0).createCell(0).setCellValue("入库单号:" + order.getBillNo());
        sheet.createRow(1).createCell(0).setCellValue("供应商:" + (order.getSupplier() != null ? order.getSupplier() : ""));
        sheet.createRow(2).createCell(0).setCellValue("状态:" + (order.getOrderStatus() == 1 ? "已入库" : order.getOrderStatus() == 2 ? "已拒绝" : "待审批"));

        Row header = sheet.createRow(4);
        String[] heads = {"物料编码", "物料名称", "批次号", "入库数量"};
        for (int i = 0; i < heads.length; i++) header.createCell(i).setCellValue(heads[i]);
        int rowNum = 5;
        for (InStorageItem item : items) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(item.getMaterialCode());
            row.createCell(1).setCellValue("");
            row.createCell(2).setCellValue(item.getBatchNo());
            row.createCell(3).setCellValue(item.getNum() != null ? item.getNum().toString() : "0");
        }
        try (OutputStream os = response.getOutputStream()) {
            wb.write(os);
        }
        wb.close();
    }
}
