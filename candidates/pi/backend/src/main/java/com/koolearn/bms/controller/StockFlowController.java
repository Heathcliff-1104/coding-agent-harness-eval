package com.koolearn.bms.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.koolearn.bms.annotation.RequireRole;
import com.koolearn.bms.dto.StockFlowDTO;
import com.koolearn.bms.mapper.StockFlowMapper;
import com.koolearn.bms.util.Result;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.net.URLEncoder;
import java.time.LocalDate;
import java.util.List;

@RequireRole({"admin", "warehouse"})
@RestController
@RequestMapping("/stockFlow")
public class StockFlowController {

    private final StockFlowMapper stockFlowMapper;

    public StockFlowController(StockFlowMapper stockFlowMapper) {
        this.stockFlowMapper = stockFlowMapper;
    }

    @GetMapping("/page")
    public Result<IPage<StockFlowDTO>> page(@RequestParam(defaultValue = "1") Long pageNum,
                                            @RequestParam(defaultValue = "10") Long pageSize,
                                            @RequestParam(required = false) String keyword,
                                            @RequestParam(required = false) String materialCode,
                                            @RequestParam(required = false) Long materialId,
                                            @RequestParam(required = false) String startTime,
                                            @RequestParam(required = false) String endTime,
                                            @RequestParam(required = false) String type) {
        Page<StockFlowDTO> page = new Page<>(pageNum, pageSize);
        return Result.success(stockFlowMapper.selectFlowPage(page, keyword, materialCode, materialId,
                startTime, endTime, type));
    }

    @GetMapping("/export")
    public void export(@RequestParam(required = false) String keyword,
                       @RequestParam(required = false) String materialCode,
                       @RequestParam(required = false) Long materialId,
                       @RequestParam(required = false) String startTime,
                       @RequestParam(required = false) String endTime,
                       @RequestParam(required = false) String type,
                       HttpServletResponse response) throws Exception {
        List<StockFlowDTO> list = stockFlowMapper.selectFlowAll(keyword, materialCode, materialId,
                startTime, endTime, type);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode("库存流水_" + LocalDate.now() + ".xlsx", "UTF-8"));
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("库存流水");
            String[] heads = {"类型", "单据号", "物料ID", "批次号", "数量", "操作人", "时间"};
            Row hr = sheet.createRow(0);
            for (int i = 0; i < heads.length; i++) hr.createCell(i).setCellValue(heads[i]);
            int n = 1;
            for (StockFlowDTO dto : list) {
                Row row = sheet.createRow(n++);
                row.createCell(0).setCellValue("in".equals(dto.getRecordType()) ? "入库" : "出库");
                row.createCell(1).setCellValue(dto.getBillNo() == null ? "" : dto.getBillNo());
                row.createCell(2).setCellValue(dto.getMaterialId() == null ? "" : dto.getMaterialId().toString());
                row.createCell(3).setCellValue(dto.getBatchNo() == null ? "" : dto.getBatchNo());
                row.createCell(4).setCellValue(dto.getNum() == null ? "" : dto.getNum().toString());
                row.createCell(5).setCellValue(dto.getOperator() == null ? "" : dto.getOperator());
                row.createCell(6).setCellValue(dto.getOpTime() == null ? "" : dto.getOpTime().toString());
            }
            wb.write(response.getOutputStream());
        }
    }
}
