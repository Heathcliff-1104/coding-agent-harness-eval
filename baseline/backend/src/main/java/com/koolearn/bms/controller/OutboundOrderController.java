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
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/outbound")
public class OutboundOrderController {

    private final OutboundOrderService outboundOrderService;
    private final OutStorageItemService outStorageItemService;

    public OutboundOrderController(OutboundOrderService outboundOrderService,
                                    OutStorageItemService outStorageItemService) {
        this.outboundOrderService = outboundOrderService;
        this.outStorageItemService = outStorageItemService;
    }

    @PostMapping("/saveDraft")
    public Result<Void> saveDraft(@RequestBody OutboundOrderDTO dto) {
        outboundOrderService.saveDraft(dto);
        return Result.success();
    }

    @PutMapping("/editDraft/{id}")
    public Result<Void> editDraft(@PathVariable Long id, @RequestBody OutboundOrderDTO dto) {
        outboundOrderService.editDraft(id, dto);
        return Result.success();
    }

    @GetMapping("/get/{id}")
    public Result<OutboundOrderDTO> getDetail(@PathVariable Long id) {
        return Result.success(outboundOrderService.getDetailById(id));
    }

    @GetMapping("/page")
    public Result<Page<OutboundOrder>> getPage(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String outboundCode,
            @RequestParam(required = false) Integer outType,
            @RequestParam(required = false) Integer orderStatus,
            @RequestParam(required = false) String keyword) {
        return Result.success(outboundOrderService.getOrderPage(pageNum, pageSize, outboundCode, outType, orderStatus, keyword));
    }

    @PostMapping("/saveOrder")
    public Result<String> saveOrder(@RequestBody OutboundOrderDTO dto) {
        return Result.success(outboundOrderService.saveOrder(dto));
    }

    @PostMapping("/dingtalk/callback")
    public Result<String> dingTalkCallback(@RequestBody Map<String, Object> params) {
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
            outboundOrderService.updateStatus(order.getId(), 0);
        } else if ("refuse".equals(result)) {
            outboundOrderService.updateStatus(order.getId(), 2);
        }
        return Result.success("回调处理成功");
    }

    @RequireRole({"admin", "warehouse"})
    @PostMapping("/confirm/{id}")
    public Result<Void> confirmOut(@PathVariable Long id, @RequestParam String operUser) {
        outboundOrderService.confirmOut(id, operUser);
        return Result.success();
    }

    @PostMapping("/reject/{id}")
    public Result<Void> rejectOut(@PathVariable Long id) {
        outboundOrderService.rejectOut(id);
        return Result.success();
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
