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
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import com.koolearn.bms.annotation.RequirePermission;
import com.koolearn.bms.service.SysOperationLogService;
import org.springframework.web.bind.annotation.RequestAttribute;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/inbound")
@RequireRole({"admin", "warehouse"})
public class InboundOrderController {

    private final InboundOrderService inboundOrderService;
    private final InStorageItemService inStorageItemService;
    private final SysOperationLogService sysLogService;

    public InboundOrderController(InboundOrderService inboundOrderService,
                                   InStorageItemService inStorageItemService,
                                   SysOperationLogService sysLogService) {
        this.inboundOrderService = inboundOrderService;
        this.inStorageItemService = inStorageItemService;
        this.sysLogService = sysLogService;
    }

    private String getIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) ip = request.getHeader("X-Real-IP");
        if (ip == null || ip.isEmpty()) ip = request.getRemoteAddr();
        return ip;
    }

    @PostMapping("/saveDraft")
    public Result<Void> saveDraft(@RequestBody InboundOrderDTO dto) {
        inboundOrderService.saveDraft(dto);
        return Result.success();
    }

    @RequireRole({"admin", "warehouse"})
    @RequirePermission("btn:inbound:confirm")
    @PostMapping("/confirm/{id}")
    public Result<Void> confirmIn(@PathVariable Long id,
                                  @RequestAttribute("username") String operator,
                                  HttpServletRequest request) {
        // 操作人取自登录令牌，防止审计伪造
        inboundOrderService.confirmIn(id, operator);
        sysLogService.log(operator, "入库", "确认入库单: " + id + " 操作人: " + operator, getIp(request));
        return Result.success();
    }

    @PutMapping("/editDraft/{id}")
    public Result<Void> editDraft(@PathVariable Long id, @RequestBody InboundOrderDTO dto) {
        inboundOrderService.editDraft(dto, id);
        return Result.success();
    }

    @GetMapping("/ding/{instanceId}")
    public Result<InboundOrder> getByDing(@PathVariable String instanceId) {
        return Result.success(inboundOrderService.getByDingInstanceId(instanceId));
    }

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
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer inType,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String materialName) {
        return Result.success(inboundOrderService.getOrderPage(pageNum, pageSize, billNo, supplier, status, keyword, inType,
                startDate, endDate, materialName));
    }

    /** 批量审核入库单：ids 逗号分隔，status 1通过(入库) / 2拒绝 */
    @RequirePermission("btn:inbound:batchAudit")
    @PostMapping("/batchAudit")
    public Result<String> batchAudit(@RequestParam String ids, @RequestParam Integer status,
                                     @RequestAttribute("username") String operator,
                                     @RequestAttribute("userId") Long userId,
                                     HttpServletRequest request) {
        int ok = 0;
        for (String idStr : ids.split(",")) {
            try {
                Long id = Long.valueOf(idStr.trim());
                inboundOrderService.updateStatus(id, status);
                ok++;
            } catch (Exception ignore) {
                // 跳过无效ID
            }
        }
        sysLogService.log(operator, "批量审核入库", "批量审核入库单 " + ok + " 张，结果:" + (status == 1 ? "通过" : "拒绝"), getIp(request));
        return Result.success("批量处理成功 " + ok + " 张");
    }

    @GetMapping("/get/{id}")
    public Result<InboundOrderDTO> getDetail(@PathVariable Long id) {
        return Result.success(inboundOrderService.getDetailById(id));
    }

    @PostMapping("/saveOrder")
    public Result<String> saveOrder(@RequestBody InboundOrderDTO dto) {
        return Result.success(inboundOrderService.saveOrder(dto));
    }

    /** 读取钉钉已审批的入库申请（需求 2.2.1）：演示模式下生成确定的示例单据 */
    @PostMapping("/dingtalk/pull")
    public Result<InboundOrder> dingtalkPull(@RequestParam(defaultValue = "1") Integer inType,
                                             @RequestAttribute("username") String operator) {
        String billNo = inType == 2 ? "DING-DEMO-RETURN-001" : "DING-DEMO-PURCHASE-001";
        InboundOrder exist = inboundOrderService.getOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<InboundOrder>()
                .eq(InboundOrder::getBillNo, billNo).last("limit 1"));
        if (exist != null) {
            return Result.success(exist);
        }
        InboundOrderDTO dto = new InboundOrderDTO();
        dto.setBillNo(billNo);
        dto.setInType(inType);
        dto.setSupplier("演示供应商");
        dto.setUserName(operator);
        dto.setRemark("从钉钉审批单自动读取（演示数据）");
        InStorageItem item = new InStorageItem();
        item.setMaterialName(inType == 2 ? "演示退库物料" : "演示采购物料");
        item.setPackageType("0603");
        item.setValueData("DEMO-1");
        item.setSpecModel("DEMO-SPEC");
        item.setManufacturer("演示厂家");
        item.setBatchNo("DEMO-BATCH");
        item.setNum(new java.math.BigDecimal("50"));
        item.setLocationNo("A-01");
        dto.setItemList(java.util.Collections.singletonList(item));
        inboundOrderService.saveOrder(dto);
        return Result.success(inboundOrderService.getOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<InboundOrder>()
                .eq(InboundOrder::getBillNo, billNo).last("limit 1")));
    }

    @PostMapping("/dingtalk/callback")
    public Result<String> dingTalkCallback(@RequestBody Map<String, Object> params) {
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
            inboundOrderService.updateStatus(order.getId(), 0);
        } else if ("refuse".equals(result)) {
            inboundOrderService.updateStatus(order.getId(), 2);
        }
        return Result.success("回调处理成功");
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
        String[] heads = {"物料编码", "物料名称", "批次号", "入库数量", "存放货位"};
        for (int i = 0; i < heads.length; i++) header.createCell(i).setCellValue(heads[i]);
        int rowNum = 5;
        for (InStorageItem item : items) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(item.getMaterialCode() != null ? item.getMaterialCode() : "");
            row.createCell(1).setCellValue(item.getMaterialName() != null ? item.getMaterialName() : "");
            row.createCell(2).setCellValue(item.getBatchNo() != null ? item.getBatchNo() : "");
            row.createCell(3).setCellValue(item.getNum() != null ? item.getNum().toString() : "0");
            row.createCell(4).setCellValue(item.getLocationNo() != null ? item.getLocationNo() : "");
        }
        try (OutputStream os = response.getOutputStream()) {
            wb.write(os);
        }
        wb.close();
    }
}
