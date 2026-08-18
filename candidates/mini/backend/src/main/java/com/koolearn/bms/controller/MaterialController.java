package com.koolearn.bms.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.koolearn.bms.annotation.RequireRole;
import com.koolearn.bms.entity.Material;
import com.koolearn.bms.service.MaterialService;
import com.koolearn.bms.util.Result;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import javax.servlet.http.HttpServletResponse;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/material")
public class MaterialController {

    private final MaterialService materialService;

    public MaterialController(MaterialService materialService) {
        this.materialService = materialService;
    }

    @GetMapping("/{id}")
    public Result<Material> getById(@PathVariable Long id) {
        return Result.success(materialService.getById(id));
    }

    @GetMapping("/list")
    public Result<List<Material>> listAll() {
        return Result.success(materialService.listAll());
    }

    @GetMapping("/page")
    public Result<IPage<Material>> page(@RequestParam(defaultValue = "1") Long pageNum,
                                        @RequestParam(defaultValue = "10") Long pageSize,
                                        @RequestParam(required = false) String materialCode,
                                        @RequestParam(required = false) String materialName,
                                        @RequestParam(required = false) String warehouseCode,
                                        @RequestParam(required = false) String packageType,
                                        @RequestParam(required = false) String keyword) {
        return Result.success(materialService.pageQuery(pageNum, pageSize, materialCode, materialName, warehouseCode, packageType, keyword));
    }

    @RequireRole({"admin", "warehouse"})
    @PostMapping("/add")
    public Result<String> add(@Valid @RequestBody Material material) {
        return materialService.save(material) ? Result.success("新增成功") : Result.fail("新增失败");
    }

    @RequireRole({"admin", "warehouse"})
    @PutMapping("/update")
    public Result<String> update(@Valid @RequestBody Material material) {
        return materialService.updateById(material) ? Result.success("更新成功") : Result.fail("更新失败");
    }

    @RequireRole("admin")
    @DeleteMapping("/del/{id}")
    public Result<String> delete(@PathVariable Long id) {
        return materialService.removeById(id) ? Result.success("删除成功") : Result.fail("删除失败");
    }

    @RequireRole({"admin", "warehouse"})
    @PutMapping("/threshold")
    public Result<String> updateThreshold(@RequestParam Long id,
                                          @RequestParam(required = false) java.math.BigDecimal minStock,
                                          @RequestParam(required = false) java.math.BigDecimal maxStock) {
        Material m = materialService.getById(id);
        if (m == null) return Result.fail("物料不存在");
        if (minStock != null) m.setMinStock(minStock);
        if (maxStock != null) m.setMaxStock(maxStock);
        materialService.updateById(m);
        return Result.success("预警阈值已更新");
    }

    @GetMapping("/export")
    public void export(@RequestParam(required = false) String materialCode,
                       @RequestParam(required = false) String materialName,
                       @RequestParam(required = false) String warehouseCode,
                       @RequestParam(required = false) String packageType,
                       @RequestParam(required = false) String keyword,
                       HttpServletResponse response) throws Exception {
        IPage<Material> page = materialService.pageQuery(1L, 100000L, materialCode, materialName, warehouseCode, packageType, keyword);
        List<Material> data = page.getRecords();
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode("库存明细_" + LocalDate.now() + ".xlsx", "UTF-8"));
        Workbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet("库存明细");
        String[] headers = {"物料编码", "物料名称", "封装", "Value值", "规格型号", "厂家批次", "库存数量", "存放货位", "备注"};
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) headerRow.createCell(i).setCellValue(headers[i]);
        int rowNum = 1;
        for (Material m : data) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(m.getMaterialCode() == null ? "" : m.getMaterialCode());
            row.createCell(1).setCellValue(m.getMaterialName() == null ? "" : m.getMaterialName());
            row.createCell(2).setCellValue(m.getPackageType() == null ? "" : m.getPackageType());
            row.createCell(3).setCellValue(m.getValueData() == null ? "" : m.getValueData());
            row.createCell(4).setCellValue(m.getSpecModel() == null ? "" : m.getSpecModel());
            row.createCell(5).setCellValue("");
            row.createCell(6).setCellValue(m.getStock() == null ? "" : m.getStock().toString());
            row.createCell(7).setCellValue(m.getLocationNo() == null ? "" : m.getLocationNo());
            row.createCell(8).setCellValue(m.getRemark() == null ? "" : m.getRemark());
        }
        try (OutputStream os = response.getOutputStream()) {
            wb.write(os);
        }
        wb.close();
    }
}
