package com.koolearn.bms.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.koolearn.bms.entity.OutRecord;
import com.koolearn.bms.service.OutRecordService;
import com.koolearn.bms.util.Result;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/outRecord")
public class OutRecordController {

    private final OutRecordService outRecordService;

    public OutRecordController(OutRecordService outRecordService) {
        this.outRecordService = outRecordService;
    }

    /** 数据范围：admin/warehouse 看全部；其他角色只能看自己操作的记录 */
    private boolean isRestricted(String role) {
        return !"admin".equals(role) && !"warehouse".equals(role);
    }

    private LambdaQueryWrapper<OutRecord> buildWrapper(String outboundCode, Long materialId,
                                                      String startTime, String endTime, String outUser) {
        LambdaQueryWrapper<OutRecord> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(outboundCode)) wrapper.like(OutRecord::getOutboundCode, outboundCode);
        if (materialId != null) wrapper.eq(OutRecord::getMaterialId, materialId);
        if (StringUtils.hasText(startTime)) wrapper.ge(OutRecord::getOutTime, startTime);
        if (StringUtils.hasText(endTime)) wrapper.le(OutRecord::getOutTime, endTime);
        if (StringUtils.hasText(outUser)) wrapper.eq(OutRecord::getOutUser, outUser);
        wrapper.orderByDesc(OutRecord::getOutTime);
        return wrapper;
    }

    @GetMapping("/page")
    public Result<IPage<OutRecord>> pageList(
            @RequestParam(defaultValue = "1") Long pageNum,
            @RequestParam(defaultValue = "10") Long pageSize,
            @RequestParam(required = false) String outboundCode,
            @RequestParam(required = false) Long materialId,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestAttribute(value = "username", required = false) String operator,
            @RequestAttribute(value = "role", required = false) String role) {
        Page<OutRecord> page = new Page<>(pageNum, pageSize);
        String outUser = isRestricted(role) ? operator : null;
        return Result.success(outRecordService.page(page, buildWrapper(outboundCode, materialId, startTime, endTime, outUser)));
    }

    /** 导出：报表名称+yyyyMMdd_HHmmss.xlsx，过滤条件与分页一致 */
    @GetMapping("/export")
    public void export(@RequestParam(required = false) String outboundCode,
                       @RequestParam(required = false) Long materialId,
                       @RequestParam(required = false) String startTime,
                       @RequestParam(required = false) String endTime,
                       @RequestAttribute(value = "username", required = false) String operator,
                       @RequestAttribute(value = "role", required = false) String role,
                       HttpServletResponse response) throws Exception {
        String outUser = isRestricted(role) ? operator : null;
        List<OutRecord> list = outRecordService.list(buildWrapper(outboundCode, materialId, startTime, endTime, outUser));
        String reportName = "出库记录";
        byte[] xlsx = buildXlsx(list);
        String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(reportName + "_" + stamp + ".xlsx", "UTF-8"));
        try (OutputStream os = response.getOutputStream()) {
            os.write(xlsx);
        }
    }

    private byte[] buildXlsx(List<OutRecord> list) throws Exception {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("出库记录");
            String[] heads = {"出库单号", "物料ID", "批次号", "出库数量", "操作人", "出库时间"};
            Row hr = sheet.createRow(0);
            for (int i = 0; i < heads.length; i++) hr.createCell(i).setCellValue(heads[i]);
            int n = 1;
            if (list != null) {
                for (OutRecord r : list) {
                    Row row = sheet.createRow(n++);
                    row.createCell(0).setCellValue(r.getOutboundCode() == null ? "" : r.getOutboundCode());
                    row.createCell(1).setCellValue(r.getMaterialId() == null ? "" : r.getMaterialId().toString());
                    row.createCell(2).setCellValue(r.getBatchNo() == null ? "" : r.getBatchNo());
                    row.createCell(3).setCellValue(r.getOutNum() == null ? "" : r.getOutNum().toString());
                    row.createCell(4).setCellValue(r.getOutUser() == null ? "" : r.getOutUser());
                    row.createCell(5).setCellValue(r.getOutTime() == null ? "" : r.getOutTime().toString());
                }
            }
            wb.write(bos);
            return bos.toByteArray();
        }
    }
}
