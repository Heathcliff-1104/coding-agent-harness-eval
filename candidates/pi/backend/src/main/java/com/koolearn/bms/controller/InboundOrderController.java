package com.koolearn.bms.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.koolearn.bms.annotation.RequireRole;
import com.koolearn.bms.dto.InboundOrderDTO;
import com.koolearn.bms.entity.InStorageItem;
import com.koolearn.bms.entity.InboundOrder;
import com.koolearn.bms.service.InboundOrderService;
import com.koolearn.bms.service.InStorageItemService;
import com.koolearn.bms.util.Result;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.servlet.http.HttpServletResponse;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/inbound")
public class InboundOrderController {

    @Value("${dingtalk.callback.token}")
    private String callbackToken;

    /** 常量时间比较，防时序侧信道 */
    private boolean callbackTokenValid(String headerToken, String paramToken) {
        String token = headerToken != null ? headerToken : paramToken;
        if (token == null || callbackToken == null) return false;
        return MessageDigest.isEqual(token.getBytes(StandardCharsets.UTF_8),
                callbackToken.getBytes(StandardCharsets.UTF_8));
    }

    private final InboundOrderService inboundOrderService;
    private final InStorageItemService inStorageItemService;
    private final com.koolearn.bms.service.SysOperationLogService sysLogService;

    public InboundOrderController(InboundOrderService inboundOrderService,
                                   InStorageItemService inStorageItemService,
                                   com.koolearn.bms.service.SysOperationLogService sysLogService) {
        this.inboundOrderService = inboundOrderService;
        this.inStorageItemService = inStorageItemService;
        this.sysLogService = sysLogService;
    }

    /** 数据范围：admin/warehouse 看全部；其他角色只能看自己的申请单 */
    private boolean isRestricted(String role) {
        return !"admin".equals(role) && !"warehouse".equals(role);
    }

    @RequireRole({"admin", "warehouse"})
    @PostMapping("/saveDraft")
    public Result<Void> saveDraft(@Valid @RequestBody InboundOrderDTO dto) {
        inboundOrderService.saveDraft(dto);
        return Result.success();
    }

    @RequireRole({"admin", "warehouse"})
    @PostMapping("/confirm/{id}")
    public Result<Void> confirmIn(@PathVariable Long id, @RequestParam String operUser,
                                  @RequestAttribute(value = "username", required = false) String operator,
                                  javax.servlet.http.HttpServletRequest request) {
        inboundOrderService.confirmIn(id, operUser);
        InboundOrder order = inboundOrderService.getById(id);
        sysLogService.log(operator != null ? operator : operUser, "入库确认", "确认入库: " + (order != null ? order.getBillNo() : id), getIp(request));
        return Result.success();
    }

    @RequireRole({"admin", "warehouse"})
    @PutMapping("/editDraft/{id}")
    public Result<Void> editDraft(@PathVariable Long id, @Valid @RequestBody InboundOrderDTO dto) {
        inboundOrderService.editDraft(dto, id);
        return Result.success();
    }

    @GetMapping("/ding/{instanceId}")
    public Result<InboundOrder> getByDing(@PathVariable String instanceId,
                                          @RequestAttribute(value = "username", required = false) String operator,
                                          @RequestAttribute(value = "role", required = false) String role) {
        InboundOrder order = inboundOrderService.getByDingInstanceId(instanceId);
        if (order == null) {
            return Result.fail("单据不存在");
        }
        if (isRestricted(role) && !operator.equals(order.getApplyUser())) {
            return Result.fail(403, "只能查看自己的入库单");
        }
        return Result.success(order);
    }

    @GetMapping("/page")
    public Result<IPage<InboundOrder>> getOrderPage(
            @RequestParam(defaultValue = "1") Long pageNum,
            @RequestParam(defaultValue = "10") Long pageSize,
            @RequestParam(required = false) String billNo,
            @RequestParam(required = false) String supplier,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword,
            @RequestAttribute(value = "username", required = false) String operator,
            @RequestAttribute(value = "role", required = false) String role) {
        String applyUser = isRestricted(role) ? operator : null;
        return Result.success(inboundOrderService.getOrderPage(pageNum, pageSize, billNo, supplier, status, keyword, applyUser));
    }

    @GetMapping("/get/{id}")
    public Result<InboundOrderDTO> getDetail(@PathVariable Long id,
                                             @RequestAttribute(value = "username", required = false) String operator,
                                             @RequestAttribute(value = "role", required = false) String role) {
        if (isRestricted(role)) {
            InboundOrder order = inboundOrderService.getById(id);
            if (order == null || !operator.equals(order.getApplyUser())) {
                return Result.fail(403, "只能查看自己的入库单");
            }
        }
        return Result.success(inboundOrderService.getDetailById(id));
    }

    @RequireRole({"admin", "warehouse"})
    @PostMapping("/saveOrder")
    public Result<String> saveOrder(@Valid @RequestBody InboundOrderDTO dto) {
        return Result.success(inboundOrderService.saveOrder(dto));
    }

    @PostMapping("/dingtalk/callback")
    public Result<String> dingTalkCallback(@RequestHeader(value = "X-Callback-Token", required = false) String headerToken,
                                          @RequestParam(value = "token", required = false) String paramToken,
                                          @RequestBody Map<String, Object> params) {
        if (!callbackTokenValid(headerToken, paramToken)) {
            return Result.fail(403, "回调验签失败：Token 无效");
        }
        String instanceId = (String) params.get("instanceId");
        String result = (String) params.get("result");
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
            inboundOrderService.approveFromCallback(order.getId());
        } else if ("refuse".equals(result)) {
            inboundOrderService.refuseFromCallback(order.getId());
        }
        return Result.success("回调处理成功");
    }

    @RequireRole({"admin", "warehouse"})
    @PostMapping("/batchConfirm")
    public Result<Integer> batchConfirm(@RequestBody List<Long> ids, @RequestParam String operUser,
                                        @RequestAttribute(value = "username", required = false) String operator,
                                        javax.servlet.http.HttpServletRequest request) {
        if (ids == null || ids.isEmpty()) {
            return Result.fail("请选择要审核的入库单");
        }
        int success = 0;
        StringBuilder errors = new StringBuilder();
        for (Long id : ids) {
            try {
                inboundOrderService.confirmIn(id, operUser);
                success++;
            } catch (Exception e) {
                errors.append("单号[").append(id).append("]:").append(e.getMessage()).append("; ");
            }
        }
        sysLogService.log(operator != null ? operator : operUser, "批量审核入库", "批量确认入库 " + success + "/" + ids.size() + " 单", getIp(request));
        if (success == ids.size()) {
            return Result.success(success);
        }
        return Result.fail("部分成功：" + success + "/" + ids.size() + "，失败原因：" + errors);
    }

    private String getIp(javax.servlet.http.HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) ip = request.getHeader("X-Real-IP");
        if (ip == null || ip.isEmpty()) ip = request.getRemoteAddr();
        return ip;
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
