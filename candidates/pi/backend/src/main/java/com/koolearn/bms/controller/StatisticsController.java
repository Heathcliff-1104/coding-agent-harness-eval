package com.koolearn.bms.controller;

import com.koolearn.bms.annotation.RequireRole;
import com.koolearn.bms.mapper.StatisticsMapper;
import com.koolearn.bms.util.Result;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RequireRole({"admin", "warehouse"})
@RestController
@RequestMapping("/statistics")
public class StatisticsController {

    private final StatisticsMapper statisticsMapper;

    public StatisticsController(StatisticsMapper statisticsMapper) {
        this.statisticsMapper = statisticsMapper;
    }

    @GetMapping("/inbound")
    public Result<List<Map<String, Object>>> inboundStats(@RequestParam String start,
                                                           @RequestParam String end,
                                                           @RequestParam(defaultValue = "%Y-%m-%d") String groupBy) {
        return Result.success(statisticsMapper.inboundStats(groupBy, start, end));
    }

    @GetMapping("/outbound")
    public Result<List<Map<String, Object>>> outboundStats(@RequestParam String start,
                                                            @RequestParam String end,
                                                            @RequestParam(defaultValue = "%Y-%m-%d") String groupBy) {
        return Result.success(statisticsMapper.outboundStats(groupBy, start, end));
    }

    @GetMapping("/inboundBySupplier")
    public Result<List<Map<String, Object>>> inboundBySupplier(@RequestParam String start, @RequestParam String end) {
        return Result.success(statisticsMapper.inboundStatsBySupplier(start, end));
    }

    @GetMapping("/outboundByDept")
    public Result<List<Map<String, Object>>> outboundByDept(@RequestParam String start, @RequestParam String end) {
        return Result.success(statisticsMapper.outboundStatsByDept(start, end));
    }

    @GetMapping("/materialSummary")
    public Result<List<Map<String, Object>>> materialSummary() {
        return Result.success(statisticsMapper.materialSummary());
    }

    @GetMapping("/stagnant")
    public Result<List<Map<String, Object>>> stagnant(@RequestParam(defaultValue = "90") int days) {
        String cutoff = LocalDateTime.now().minusDays(days)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        return Result.success(statisticsMapper.stagnantMaterials(cutoff));
    }

    // ====================== 导出 ======================

    @GetMapping("/exportInbound")
    public void exportInbound(@RequestParam String start, @RequestParam String end,
                              HttpServletResponse response) throws Exception {
        List<Map<String, Object>> data = statisticsMapper.inboundStats("%Y-%m-%d", start, end);
        writeExport(response, "入库统计", new String[]{"日期", "入库次数", "入库总数"}, data,
                new String[]{"period", "cnt", "total"});
    }

    @GetMapping("/exportOutbound")
    public void exportOutbound(@RequestParam String start, @RequestParam String end,
                               HttpServletResponse response) throws Exception {
        List<Map<String, Object>> data = statisticsMapper.outboundStats("%Y-%m-%d", start, end);
        writeExport(response, "出库统计", new String[]{"日期", "出库次数", "出库总数"}, data,
                new String[]{"period", "cnt", "total"});
    }

    @GetMapping("/exportStagnant")
    public void exportStagnant(@RequestParam(defaultValue = "90") int days,
                               HttpServletResponse response) throws Exception {
        String cutoff = LocalDateTime.now().minusDays(days)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        List<Map<String, Object>> data = statisticsMapper.stagnantMaterials(cutoff);
        writeExport(response, "呆滞物品", new String[]{"物料编码", "物料名称", "当前库存", "库存金额", "最后出库时间"}, data,
                new String[]{"materialCode", "materialName", "stock", "amount", "lastOutTime"});
    }

    @GetMapping("/exportInventory")
    public void exportInventory(@RequestParam(required = false) String code,
                                @RequestParam(required = false) String name,
                                HttpServletResponse response) throws Exception {
        List<Map<String, Object>> data = statisticsMapper.materialSummary();
        writeExport(response, "库存明细", new String[]{"物料编码", "物料名称", "封装", "规格型号", "库存数量",
                        "占用", "存放货位", "累计入库", "入库次数", "累计出库", "出库次数", "最后出库时间"},
                data, new String[]{"materialCode", "materialName", "packageType", "specModel", "stock",
                        "lockStock", "locationNo", "totalIn", "inCount", "totalOut", "outCount", "lastOutTime"});
    }

    /** 导出：报表名称+yyyyMMdd_HHmmss；超过 50000 行打包为 zip（内含一个 xlsx） */
    private void writeExport(HttpServletResponse response, String reportName, String[] headers,
                             List<Map<String, Object>> data, String[] keys) throws Exception {
        byte[] xlsx = buildXlsx(reportName, headers, data, keys);
        String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        if (data != null && data.size() > 50000) {
            response.setContentType("application/zip");
            response.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(reportName + "_" + stamp + ".zip", "UTF-8"));
            try (ZipOutputStream zos = new ZipOutputStream(response.getOutputStream())) {
                zos.putNextEntry(new ZipEntry(reportName + "_" + stamp + ".xlsx"));
                zos.write(xlsx);
                zos.closeEntry();
            }
        } else {
            response.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(reportName + "_" + stamp + ".xlsx", "UTF-8"));
            try (OutputStream os = response.getOutputStream()) {
                os.write(xlsx);
            }
        }
    }

    private byte[] buildXlsx(String sheetName, String[] headers,
                             List<Map<String, Object>> data, String[] keys) throws Exception {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet(sheetName.length() > 31 ? sheetName.substring(0, 31) : sheetName);
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }
            if (data != null) {
                for (int i = 0; i < data.size(); i++) {
                    Row row = sheet.createRow(i + 1);
                    Map<String, Object> rowData = data.get(i);
                    for (int j = 0; j < keys.length; j++) {
                        Object val = rowData.get(keys[j]);
                        row.createCell(j).setCellValue(val != null ? val.toString() : "");
                    }
                }
            }
            wb.write(bos);
            return bos.toByteArray();
        }
    }
}
