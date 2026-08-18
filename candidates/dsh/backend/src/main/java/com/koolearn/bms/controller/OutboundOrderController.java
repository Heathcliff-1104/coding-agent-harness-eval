package com.koolearn.bms.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.koolearn.bms.annotation.RequireRole;
import com.koolearn.bms.dto.OutboundOrderDTO;
import com.koolearn.bms.entity.BomPlan;
import com.koolearn.bms.entity.OutStorageItem;
import com.koolearn.bms.entity.OutboundOrder;
import com.koolearn.bms.service.BomPlanService;
import com.koolearn.bms.service.MaterialService;
import com.koolearn.bms.service.OutboundOrderService;
import com.koolearn.bms.service.OutStorageItemService;
import com.koolearn.bms.service.SysOperationLogService;
import com.koolearn.bms.annotation.RequirePermission;
import com.koolearn.bms.util.Result;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
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
    private final BomPlanService bomPlanService;
    private final SysOperationLogService sysLogService;
    private final MaterialService materialService;

    public OutboundOrderController(OutboundOrderService outboundOrderService,
                                    OutStorageItemService outStorageItemService,
                                    BomPlanService bomPlanService,
                                    SysOperationLogService sysLogService,
                                    MaterialService materialService) {
        this.outboundOrderService = outboundOrderService;
        this.outStorageItemService = outStorageItemService;
        this.bomPlanService = bomPlanService;
        this.sysLogService = sysLogService;
        this.materialService = materialService;
    }

    // ==================== BOM（需求 2.3.2 生产领料） ====================

    /** 导入BOM表并逐项匹配库存（不落库） */
    @PostMapping("/bom/match")
    public Result<List<Map<String, Object>>> bomMatch(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) body.get("items");
        if (items == null || items.isEmpty()) return Result.fail("BOM明细不能为空");
        return Result.success(bomPlanService.matchBom(items));
    }

    /** 导入BOM表：匹配库存并保存为备料计划单（记录BOM版本） */
    @PostMapping("/bom/import")
    public Result<Map<String, Object>> bomImport(@RequestBody Map<String, Object> body,
                                                 @RequestAttribute("username") String operator,
                                                 HttpServletRequest request) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) body.get("items");
        if (items == null || items.isEmpty()) return Result.fail("BOM明细不能为空");
        String bomVersion = body.get("bomVersion") == null ? null : body.get("bomVersion").toString();
        List<Map<String, Object>> matched = bomPlanService.matchBom(items);
        String planNo = bomPlanService.savePlan(bomVersion, matched, operator);
        Map<String, Object> data = new java.util.HashMap<>();
        data.put("planNo", planNo);
        data.put("items", matched);
        sysLogService.log(operator, "BOM备料", "导入BOM并保存备料计划单: " + planNo, getIp(request));
        return Result.success(data);
    }

    /** 备料计划单历史（BOM版本可查） */
    @GetMapping("/bom/plan")
    public Result<IPage<BomPlan>> bomPlanPage(@RequestParam(defaultValue = "1") Long pageNum,
                                              @RequestParam(defaultValue = "10") Long pageSize,
                                              @RequestParam(required = false) String bomVersion) {
        return Result.success(bomPlanService.pagePlans(pageNum, pageSize, bomVersion));
    }

    private String getIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) ip = request.getHeader("X-Real-IP");
        if (ip == null || ip.isEmpty()) ip = request.getRemoteAddr();
        return ip;
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
            @RequestParam(required = false) String keyword,
            @RequestAttribute("username") String operator,
            @RequestAttribute("dataScope") String dataScope) {
        return Result.success(outboundOrderService.getOrderPage(pageNum, pageSize, outboundCode, outType, orderStatus, keyword, operator, dataScope));
    }

    @PostMapping("/saveOrder")
    public Result<String> saveOrder(@RequestBody OutboundOrderDTO dto) {
        return Result.success(outboundOrderService.saveOrder(dto));
    }

    /** 读取钉钉已审批的领用&出库申请（需求 2.3.1）：演示模式下生成确定的示例单据 */
    @PostMapping("/dingtalk/pull")
    public Result<OutboundOrder> dingtalkPull(@RequestAttribute("username") String operator) {
        OutboundOrder exist = outboundOrderService.getOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<OutboundOrder>()
                .eq(OutboundOrder::getOutboundCode, "J" + java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE) + "-001")
                .eq(OutboundOrder::getApplyUser, operator).last("limit 1"));
        if (exist != null) return Result.success(exist);
        com.koolearn.bms.dto.OutboundOrderDTO dto = new com.koolearn.bms.dto.OutboundOrderDTO();
        dto.setOutType(1);
        dto.setApplyUser(operator);
        dto.setRemark("从钉钉审批单自动读取（演示数据）");
        // 演示出库单：占用第一条物料
        com.koolearn.bms.entity.Material mat = materialService.list().stream().findFirst().orElse(null);
        if (mat != null) {
            com.koolearn.bms.dto.OutStorageItemDTO item = new com.koolearn.bms.dto.OutStorageItemDTO();
            item.setMaterialId(mat.getId());
            item.setMaterialCode(mat.getMaterialCode());
            item.setOutNum(new java.math.BigDecimal("1"));
            dto.setItemList(java.util.Collections.singletonList(item));
        }
        outboundOrderService.saveDraft(dto);
        return Result.success(outboundOrderService.getOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<OutboundOrder>()
                .eq(OutboundOrder::getApplyUser, operator)
                .orderByDesc(OutboundOrder::getCreateTime).last("limit 1")));
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
            // 驳回必须走 rejectOut：释放已占用的库存，避免锁定永久滞留
            outboundOrderService.rejectOut(order.getId());
        }
        return Result.success("回调处理成功");
    }

    @RequireRole({"admin", "warehouse"})
    @RequirePermission("btn:outbound:confirm")
    @PostMapping("/confirm/{id}")
    public Result<Void> confirmOut(@PathVariable Long id,
                                   @RequestAttribute("username") String operator,
                                   HttpServletRequest request) {
        // 操作人取自登录令牌，防止审计伪造
        outboundOrderService.confirmOut(id, operator);
        sysLogService.log(operator, "出库", "确认出库单: " + id + " 操作人: " + operator, getIp(request));
        return Result.success();
    }

    @RequireRole({"admin", "warehouse"})
    @RequirePermission("btn:outbound:reject")
    @PostMapping("/reject/{id}")
    public Result<Void> rejectOut(@PathVariable Long id, @RequestAttribute("username") String operator,
                                  HttpServletRequest request) {
        outboundOrderService.rejectOut(id);
        sysLogService.log(operator, "出库驳回", "驳回出库单: " + id, getIp(request));
        return Result.success();
    }

    @RequireRole({"admin", "warehouse"})
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
            row.createCell(0).setCellValue(item.getMaterialCode() != null ? item.getMaterialCode() : "");
            row.createCell(1).setCellValue(item.getMaterialName() != null ? item.getMaterialName() : "");
            row.createCell(2).setCellValue(item.getBatchNo() != null ? item.getBatchNo() : "");
            row.createCell(3).setCellValue(item.getOutNum() != null ? item.getOutNum().toString() : "0");
        }
        try (OutputStream os = response.getOutputStream()) {
            wb.write(os);
        }
        wb.close();
    }
}
