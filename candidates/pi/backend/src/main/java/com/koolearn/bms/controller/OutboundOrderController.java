package com.koolearn.bms.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.koolearn.bms.annotation.RequireRole;
import com.koolearn.bms.dto.OutboundOrderDTO;
import com.koolearn.bms.entity.OutStorageItem;
import com.koolearn.bms.entity.OutboundOrder;
import com.koolearn.bms.service.OutboundOrderService;
import com.koolearn.bms.service.OutStorageItemService;
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
@RequestMapping("/outbound")
public class OutboundOrderController {

    @Value("${dingtalk.callback.token}")
    private String callbackToken;

    /** 常量时间比较，防时序侧信道 */
    private boolean callbackTokenValid(String headerToken, String paramToken) {
        String token = headerToken != null ? headerToken : paramToken;
        if (token == null || callbackToken == null) return false;
        return MessageDigest.isEqual(token.getBytes(StandardCharsets.UTF_8),
                callbackToken.getBytes(StandardCharsets.UTF_8));
    }

    private final OutboundOrderService outboundOrderService;
    private final OutStorageItemService outStorageItemService;
    private final com.koolearn.bms.service.SysOperationLogService sysLogService;

    public OutboundOrderController(OutboundOrderService outboundOrderService,
                                    OutStorageItemService outStorageItemService,
                                    com.koolearn.bms.service.SysOperationLogService sysLogService) {
        this.outboundOrderService = outboundOrderService;
        this.outStorageItemService = outStorageItemService;
        this.sysLogService = sysLogService;
    }

    /** 数据范围：admin/warehouse 看全部；其他角色只能看自己的申请单 */
    private boolean isRestricted(String role) {
        return !"admin".equals(role) && !"warehouse".equals(role);
    }

    /** 出库草稿：任何登录用户（工程师提交领料申请），applyUser 强制取当前登录人 */
    @PostMapping("/saveDraft")
    public Result<Long> saveDraft(@Valid @RequestBody OutboundOrderDTO dto,
                                  @RequestAttribute(value = "username", required = false) String operator,
                                  @RequestAttribute(value = "role", required = false) String role) {
        if (isRestricted(role)) {
            dto.setApplyUser(operator);
        }
        Long id = outboundOrderService.saveDraft(dto);
        return Result.success(id);
    }

    /** 编辑草稿：仅待审批状态，非 admin/warehouse 只能改自己的单 */
    @PutMapping("/editDraft/{id}")
    public Result<Void> editDraft(@PathVariable Long id, @Valid @RequestBody OutboundOrderDTO dto,
                                  @RequestAttribute(value = "username", required = false) String operator,
                                  @RequestAttribute(value = "role", required = false) String role) {
        if (isRestricted(role)) {
            OutboundOrder order = outboundOrderService.getById(id);
            if (order == null || !operator.equals(order.getApplyUser())) {
                return Result.fail(403, "只能编辑自己的出库单");
            }
            dto.setApplyUser(operator);
        }
        outboundOrderService.editDraft(id, dto);
        return Result.success();
    }

    @GetMapping("/get/{id}")
    public Result<OutboundOrderDTO> getDetail(@PathVariable Long id,
                                              @RequestAttribute(value = "username", required = false) String operator,
                                              @RequestAttribute(value = "role", required = false) String role) {
        if (isRestricted(role)) {
            OutboundOrder order = outboundOrderService.getById(id);
            if (order == null || !operator.equals(order.getApplyUser())) {
                return Result.fail(403, "只能查看自己的出库单");
            }
        }
        return Result.success(outboundOrderService.getDetailById(id));
    }

    @GetMapping("/page")
    public Result<Page<OutboundOrder>> getPage(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String outboundCode,
            @RequestParam(required = false) Integer outType,
            @RequestParam(required = false) Integer orderStatus,
            @RequestParam(required = false) String keyword,
            @RequestAttribute(value = "username", required = false) String operator,
            @RequestAttribute(value = "role", required = false) String role) {
        String applyUser = isRestricted(role) ? operator : null;
        return Result.success(outboundOrderService.getOrderPage(pageNum, pageSize, outboundCode, outType, orderStatus, keyword, applyUser));
    }

    /** 提交审批：任何登录用户提交自己的草稿；非 admin/warehouse 校验草稿归属 */
    @PostMapping("/saveOrder")
    public Result<String> saveOrder(@Valid @RequestBody OutboundOrderDTO dto,
                                    @RequestAttribute(value = "username", required = false) String operator,
                                    @RequestAttribute(value = "role", required = false) String role) {
        if (isRestricted(role)) {
            OutboundOrder order = outboundOrderService.getById(dto.getId());
            if (order == null || !operator.equals(order.getApplyUser())) {
                return Result.fail(403, "只能提交自己的出库单");
            }
        }
        return Result.success(outboundOrderService.saveOrder(dto));
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

        OutboundOrder order = outboundOrderService.getByDingInstanceId(instanceId);
        if (order == null) {
            return Result.fail("单据不存在");
        }
        if (order.getOrderStatus() == 1 || order.getOrderStatus() == 2) {
            return Result.success("处理完成");
        }

        if ("agree".equals(result)) {
            outboundOrderService.approveFromCallback(order.getId());
        } else if ("refuse".equals(result)) {
            // 驳回必须走 rejectOut：释放锁定库存 + 状态置 2
            outboundOrderService.rejectOut(order.getId());
        }
        return Result.success("回调处理成功");
    }

    @RequireRole({"admin", "warehouse"})
    @PostMapping("/confirm/{id}")
    public Result<Void> confirmOut(@PathVariable Long id, @RequestParam String operUser,
                                   @RequestAttribute(value = "username", required = false) String operator,
                                   javax.servlet.http.HttpServletRequest request) {
        outboundOrderService.confirmOut(id, operUser);
        OutboundOrder order = outboundOrderService.getById(id);
        sysLogService.log(operator != null ? operator : operUser, "出库确认", "确认出库: " + (order != null ? order.getOutboundCode() : id), getIp(request));
        return Result.success();
    }

    @RequireRole({"admin", "warehouse"})
    @PostMapping("/reject/{id}")
    public Result<Void> rejectOut(@PathVariable Long id,
                                  @RequestAttribute(value = "username", required = false) String operator,
                                  javax.servlet.http.HttpServletRequest request) {
        outboundOrderService.rejectOut(id);
        OutboundOrder order = outboundOrderService.getById(id);
        sysLogService.log(operator != null ? operator : "system", "出库驳回", "驳回出库单: " + (order != null ? order.getOutboundCode() : id), getIp(request));
        return Result.success();
    }

    private String getIp(javax.servlet.http.HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) ip = request.getHeader("X-Real-IP");
        if (ip == null || ip.isEmpty()) ip = request.getRemoteAddr();
        return ip;
    }

    @GetMapping("/export/{id}")
    public void exportOrder(@PathVariable Long id, HttpServletResponse response) throws Exception {
        OutboundOrderDTO dto = outboundOrderService.getDetailById(id);
        if (dto == null) throw new RuntimeException("单据不存在");
        List<OutStorageItem> items = outStorageItemService.selectByOutboundId(id);

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode("出库单_" + dto.getOutboundCode() + ".xlsx", "UTF-8"));

        Workbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet("出库单");
        sheet.createRow(0).createCell(0).setCellValue("出库单号:" + dto.getOutboundCode());
        sheet.createRow(1).createCell(0).setCellValue("申请人:" + (dto.getApplyUser() != null ? dto.getApplyUser() : ""));

        Row header = sheet.createRow(3);
        String[] heads = {"物料编码", "物料名称", "批次号", "出库数量"};
        for (int i = 0; i < heads.length; i++) header.createCell(i).setCellValue(heads[i]);
        int rowNum = 4;
        for (OutStorageItem item : items) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(item.getMaterialCode());
            row.createCell(1).setCellValue("");
            row.createCell(2).setCellValue(item.getBatchNo());
            row.createCell(3).setCellValue(item.getOutNum() != null ? item.getOutNum().toString() : "0");
        }
        try (OutputStream os = response.getOutputStream()) {
            wb.write(os);
        }
        wb.close();
    }
}
