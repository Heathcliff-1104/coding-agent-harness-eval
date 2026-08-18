package com.koolearn.bms.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.koolearn.bms.entity.InRecord;
import com.koolearn.bms.service.InRecordService;
import com.koolearn.bms.util.Result;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/inRecord")
public class InRecordController {

    private final InRecordService inRecordService;

    public InRecordController(InRecordService inRecordService) {
        this.inRecordService = inRecordService;
    }

    /** 数据范围：admin/warehouse 看全部；其他角色只能看自己操作的记录 */
    private boolean isRestricted(String role) {
        return !"admin".equals(role) && !"warehouse".equals(role);
    }

    @GetMapping("/list")
    public Result<List<InRecord>> list(@RequestParam String startDate, @RequestParam String endDate,
                                       @RequestAttribute(value = "username", required = false) String operator,
                                       @RequestAttribute(value = "role", required = false) String role) {
        List<InRecord> list = inRecordService.selectByDate(startDate, endDate);
        if (isRestricted(role)) {
            final String me = operator;
            list.removeIf(r -> !me.equals(r.getInUser()));
        }
        return Result.success(list);
    }

    @GetMapping("/page")
    public Result<IPage<InRecord>> page(@RequestParam(defaultValue = "1") Long pageNum,
                                        @RequestParam(defaultValue = "10") Long pageSize,
                                        @RequestParam(required = false) String startDate,
                                        @RequestParam(required = false) String endDate,
                                        @RequestParam(required = false) String keyword,
                                        @RequestParam(required = false) String billNo,
                                        @RequestAttribute(value = "username", required = false) String operator,
                                        @RequestAttribute(value = "role", required = false) String role) {
        String inUser = isRestricted(role) ? operator : null;
        return Result.success(inRecordService.pageQuery(pageNum, pageSize, startDate, endDate, keyword, billNo, inUser));
    }

    /** 导出：报表名称+yyyyMMdd_HHmmss.xlsx，过滤条件与分页一致 */
    @GetMapping("/export")
    public void export(@RequestParam(required = false) String startDate,
                       @RequestParam(required = false) String endDate,
                       @RequestParam(required = false) String keyword,
                       @RequestParam(required = false) String billNo,
                       @RequestAttribute(value = "username", required = false) String operator,
                       @RequestAttribute(value = "role", required = false) String role,
                       HttpServletResponse response) throws Exception {
        String inUser = isRestricted(role) ? operator : null;
        List<InRecord> list = inRecordService.selectByCondition(startDate, endDate, keyword, billNo, inUser);
        String reportName = "入库记录";
        byte[] xlsx = buildXlsx(list);
        String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(reportName + "_" + stamp + ".xlsx", "UTF-8"));
        try (OutputStream os = response.getOutputStream()) {
            os.write(xlsx);
        }
    }

    private byte[] buildXlsx(List<InRecord> list) throws Exception {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("入库记录");
            String[] heads = {"入库单号", "物料编码", "物料名称", "批次号", "入库数量", "操作人", "货位", "入库时间"};
            Row hr = sheet.createRow(0);
            for (int i = 0; i < heads.length; i++) hr.createCell(i).setCellValue(heads[i]);
            int n = 1;
            if (list != null) {
                for (InRecord r : list) {
                    Row row = sheet.createRow(n++);
                    row.createCell(0).setCellValue(r.getBillNo() == null ? "" : r.getBillNo());
                    row.createCell(1).setCellValue(r.getMaterialCode() == null ? "" : r.getMaterialCode());
                    row.createCell(2).setCellValue(r.getMaterialName() == null ? "" : r.getMaterialName());
                    row.createCell(3).setCellValue(r.getBatchNo() == null ? "" : r.getBatchNo());
                    row.createCell(4).setCellValue(r.getInNum() == null ? "" : r.getInNum().toString());
                    row.createCell(5).setCellValue(r.getInUser() == null ? "" : r.getInUser());
                    row.createCell(6).setCellValue(r.getLocationNo() == null ? "" : r.getLocationNo());
                    row.createCell(7).setCellValue(r.getInTime() == null ? "" : r.getInTime().toString());
                }
            }
            wb.write(bos);
            return bos.toByteArray();
        }
    }
}
