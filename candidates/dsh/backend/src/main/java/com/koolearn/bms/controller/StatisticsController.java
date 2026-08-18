package com.koolearn.bms.controller;

import com.koolearn.bms.annotation.RequireRole;
import com.koolearn.bms.mapper.StatisticsMapper;
import com.koolearn.bms.service.SysOperationLogService;
import com.koolearn.bms.util.Result;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RestController
@RequestMapping("/statistics")
@RequireRole({"admin", "warehouse"})
public class StatisticsController {

    /** 超过该行数自动打包为 zip（需求 2.5.5） */
    private static final int ZIP_THRESHOLD = 50000;

    private final StatisticsMapper statisticsMapper;
    private final SysOperationLogService sysLogService;

    public StatisticsController(StatisticsMapper statisticsMapper, SysOperationLogService sysLogService) {
        this.statisticsMapper = statisticsMapper;
        this.sysLogService = sysLogService;
    }

    @GetMapping("/inbound")
    public Result<List<Map<String, Object>>> inboundStats(@RequestParam String start,
                                                           @RequestParam String end,
                                                           @RequestParam(defaultValue = "%Y-%m-%d") String groupBy) {
        return Result.success(statisticsMapper.inboundStats(validGroupBy(groupBy), start, end));
    }

    @GetMapping("/outbound")
    public Result<List<Map<String, Object>>> outboundStats(@RequestParam String start,
                                                            @RequestParam String end,
                                                            @RequestParam(defaultValue = "%Y-%m-%d") String groupBy) {
        return Result.success(statisticsMapper.outboundStats(validGroupBy(groupBy), start, end));
    }

    /** 白名单校验分组格式，防止任意格式串进入 SQL。 */
    private String validGroupBy(String fmt) {
        if (fmt == null) return "%Y-%m-%d";
        switch (fmt) {
            case "%Y":          // 年
            case "%Y-%m":       // 月
            case "%Y-%u":       // 周
            case "%Y-%m-%d":    // 日
            case "%Y-%m-%d %H": // 小时
                return fmt;
            default:
                throw new RuntimeException("不支持的分组格式: " + fmt);
        }
    }

    @GetMapping("/stagnant")
    public Result<List<Map<String, Object>>> stagnant(@RequestParam(defaultValue = "90") int days) {
        return Result.success(statisticsMapper.stagnantMaterials(days));
    }

    /** 入库统计：按物料（总数/次数/平均批次数量） */
    @GetMapping("/inbound/material")
    public Result<List<Map<String, Object>>> inboundByMaterial(@RequestParam String start,
                                                               @RequestParam String end,
                                                               @RequestParam(required = false) Long materialId) {
        return Result.success(statisticsMapper.inboundStatsByMaterial(materialId, start, end));
    }

    /** 入库统计：按供应商 */
    @GetMapping("/inbound/supplier")
    public Result<List<Map<String, Object>>> inboundBySupplier(@RequestParam String start,
                                                               @RequestParam String end) {
        return Result.success(statisticsMapper.inboundStatsBySupplier(start, end));
    }

    /** 出库统计：按物料 */
    @GetMapping("/outbound/material")
    public Result<List<Map<String, Object>>> outboundByMaterial(@RequestParam String start,
                                                                @RequestParam String end,
                                                                @RequestParam(required = false) Long materialId) {
        return Result.success(statisticsMapper.outboundStatsByMaterial(materialId, start, end));
    }

    /** 出库统计：按领料部门 */
    @GetMapping("/outbound/dept")
    public Result<List<Map<String, Object>>> outboundByDept(@RequestParam String start,
                                                            @RequestParam String end) {
        return Result.success(statisticsMapper.outboundStatsByDept(start, end));
    }

    @GetMapping("/exportInbound")
    public void exportInbound(@RequestParam String start, @RequestParam String end,
                              @RequestAttribute("username") String operator, HttpServletRequest request,
                              HttpServletResponse response) throws Exception {
        List<Map<String, Object>> data = statisticsMapper.inboundStats("%Y-%m-%d", start, end);
        exportExcel(response, "入库统计", new String[]{"日期", "入库次数", "入库总数"}, data,
                new String[]{"period", "cnt", "total"});
        sysLogService.log(operator, "导出报表", "导出入库统计", getIp(request));
    }

    @GetMapping("/exportOutbound")
    public void exportOutbound(@RequestParam String start, @RequestParam String end,
                               @RequestAttribute("username") String operator, HttpServletRequest request,
                               HttpServletResponse response) throws Exception {
        List<Map<String, Object>> data = statisticsMapper.outboundStats("%Y-%m-%d", start, end);
        exportExcel(response, "出库统计", new String[]{"日期", "出库次数", "出库总数"}, data,
                new String[]{"period", "cnt", "total"});
        sysLogService.log(operator, "导出报表", "导出出库统计", getIp(request));
    }

    @GetMapping("/exportStagnant")
    public void exportStagnant(@RequestParam(defaultValue = "90") int days,
                               @RequestAttribute("username") String operator, HttpServletRequest request,
                               HttpServletResponse response) throws Exception {
        List<Map<String, Object>> data = statisticsMapper.stagnantMaterials(days);
        exportExcel(response, "呆滞物品", new String[]{"物料编码", "物料名称", "当前库存", "库存金额", "距最后出库天数"}, data,
                new String[]{"materialCode", "materialName", "stock", "stockAmount", "lastOutDays"});
        sysLogService.log(operator, "导出报表", "导出呆滞物品", getIp(request));
    }

    @GetMapping("/exportInventoryDetail")
    public void exportInventoryDetail(@RequestAttribute("username") String operator, HttpServletRequest request,
                                      HttpServletResponse response) throws Exception {
        List<Map<String, Object>> data = statisticsMapper.inventoryDetail();
        exportExcel(response, "库存明细", new String[]{"物料编码", "物料名称", "封装", "value值", "规格型号", "厂家名称", "库存数量", "占用数量", "存放货位", "备注"}, data,
                new String[]{"materialCode", "materialName", "packageType", "valueData", "specModel", "manufacturer", "stock", "lockStock", "locationNo", "remark"});
        sysLogService.log(operator, "导出报表", "导出库存明细", getIp(request));
    }

    private void exportExcel(HttpServletResponse response, String name, String[] headers,
                             List<Map<String, Object>> data, String[] keys) throws Exception {
        byte[] xlsx = buildWorkbook(name, headers, data, keys);
        String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        if (data.size() > ZIP_THRESHOLD) {
            // 超过5万行自动压缩为 zip（需求 2.5.5）
            response.setContentType("application/zip");
            response.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(name + "_" + stamp + ".zip", "UTF-8"));
            try (ZipOutputStream zos = new ZipOutputStream(response.getOutputStream())) {
                zos.putNextEntry(new ZipEntry(name + "_" + stamp + ".xlsx"));
                zos.write(xlsx);
                zos.closeEntry();
            }
        } else {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(name + "_" + stamp + ".xlsx", "UTF-8"));
            try (OutputStream os = response.getOutputStream()) {
                os.write(xlsx);
            }
        }
    }

    private byte[] buildWorkbook(String name, String[] headers, List<Map<String, Object>> data, String[] keys) throws Exception {
        Workbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet(name);
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            headerRow.createCell(i).setCellValue(headers[i]);
        }
        for (int i = 0; i < data.size(); i++) {
            Row row = sheet.createRow(i + 1);
            Map<String, Object> rowData = data.get(i);
            for (int j = 0; j < keys.length; j++) {
                Object val = rowData.get(keys[j]);
                row.createCell(j).setCellValue(val != null ? val.toString() : "");
            }
        }
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            wb.write(bos);
            return bos.toByteArray();
        } finally {
            wb.close();
        }
    }

    private String getIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) ip = request.getHeader("X-Real-IP");
        if (ip == null || ip.isEmpty()) ip = request.getRemoteAddr();
        return ip;
    }
}
