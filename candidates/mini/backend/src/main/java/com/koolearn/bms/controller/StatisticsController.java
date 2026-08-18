package com.koolearn.bms.controller;

import com.koolearn.bms.mapper.StatisticsMapper;
import com.koolearn.bms.util.Result;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.time.LocalDate;
import java.util.*;

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

    @GetMapping("/stagnant")
    public Result<List<Map<String, Object>>> stagnant(@RequestParam(defaultValue = "90") int days) {
        return Result.success(statisticsMapper.stagnantMaterials(days));
    }

    @GetMapping("/inboundByMaterial")
    public Result<List<Map<String, Object>>> inboundByMaterial(@RequestParam String start,
                                                               @RequestParam String end,
                                                               @RequestParam(required = false) Long materialId) {
        return Result.success(statisticsMapper.inboundStatsByMaterial(materialId, start, end));
    }

    @GetMapping("/inboundBySupplier")
    public Result<List<Map<String, Object>>> inboundBySupplier(@RequestParam String start,
                                                               @RequestParam String end) {
        return Result.success(statisticsMapper.inboundStatsBySupplier(start, end));
    }

    @GetMapping("/outboundByMaterial")
    public Result<List<Map<String, Object>>> outboundByMaterial(@RequestParam String start,
                                                                @RequestParam String end,
                                                                @RequestParam(required = false) Long materialId) {
        return Result.success(statisticsMapper.outboundStatsByMaterial(materialId, start, end));
    }

    @GetMapping("/outboundByDept")
    public Result<List<Map<String, Object>>> outboundByDept(@RequestParam String start,
                                                            @RequestParam String end) {
        return Result.success(statisticsMapper.outboundStatsByDept(start, end));
    }

    @GetMapping("/exportInbound")
    public void exportInbound(@RequestParam String start, @RequestParam String end,
                              HttpServletResponse response) throws Exception {
        List<Map<String, Object>> data = statisticsMapper.inboundStats("%Y-%m-%d", start, end);
        exportExcel(response, "入库统计", new String[]{"日期", "入库次数", "入库总数"}, data,
                new String[]{"period", "cnt", "total"});
    }

    @GetMapping("/exportOutbound")
    public void exportOutbound(@RequestParam String start, @RequestParam String end,
                               HttpServletResponse response) throws Exception {
        List<Map<String, Object>> data = statisticsMapper.outboundStats("%Y-%m-%d", start, end);
        exportExcel(response, "出库统计", new String[]{"日期", "出库次数", "出库总数"}, data,
                new String[]{"period", "cnt", "total"});
    }

    @GetMapping("/exportStagnant")
    public void exportStagnant(@RequestParam(defaultValue = "90") int days,
                               HttpServletResponse response) throws Exception {
        List<Map<String, Object>> data = statisticsMapper.stagnantMaterials(days);
        exportExcel(response, "呆滞物品", new String[]{"物料编码", "物料名称", "当前库存", "库存金额", "最后出库时间", "距最后出库天数"}, data,
                new String[]{"materialCode", "materialName", "stock", "stockAmount", "lastOutTime", "lastOutDays"});
    }

    private void exportExcel(HttpServletResponse response, String name, String[] headers,
                             List<Map<String, Object>> data, String[] keys) throws Exception {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(name + "_" + LocalDate.now() + ".xlsx", "UTF-8"));

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
        try (OutputStream os = response.getOutputStream()) {
            wb.write(os);
        }
        wb.close();
    }
}
