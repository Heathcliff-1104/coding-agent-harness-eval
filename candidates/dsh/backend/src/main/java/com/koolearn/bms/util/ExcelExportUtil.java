package com.koolearn.bms.util;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * 通用 Excel 导出工具：报表名称+导出日期时间.xlsx（超过5万行自动压缩为zip）。
 */
public final class ExcelExportUtil {

    private static final int ZIP_THRESHOLD = 50000;

    private ExcelExportUtil() {
    }

    public static void export(HttpServletResponse response, String name, String[] headers,
                              List<Map<String, Object>> data, String[] keys) throws Exception {
        byte[] xlsx = buildWorkbook(name, headers, data, keys);
        String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        if (data.size() > ZIP_THRESHOLD) {
            response.setContentType("application/zip");
            response.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(name + "_" + stamp + ".zip", "UTF-8"));
            try (java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(response.getOutputStream())) {
                zos.putNextEntry(new java.util.zip.ZipEntry(name + "_" + stamp + ".xlsx"));
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

    private static byte[] buildWorkbook(String name, String[] headers, List<Map<String, Object>> data, String[] keys) throws Exception {
        Workbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet(name.length() > 30 ? name.substring(0, 30) : name);
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
}
