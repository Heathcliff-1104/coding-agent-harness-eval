package com.koolearn.bms.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.koolearn.bms.annotation.RequireRole;
import com.koolearn.bms.entity.Material;
import com.koolearn.bms.service.MaterialService;
import com.koolearn.bms.util.Result;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.math.BigDecimal;
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
                                        @RequestParam(required = false) String keyword) {
        return Result.success(materialService.pageQuery(pageNum, pageSize, materialCode, materialName, warehouseCode, keyword));
    }

    @RequireRole({"admin", "warehouse"})
    @PostMapping("/add")
    public Result<String> add(@Valid @RequestBody Material material) {
        // 安全：不允许请求体指定 id/库存/占用/版本（库存只能通过出入库流程变更）
        material.setId(null);
        material.setStock(BigDecimal.ZERO);
        material.setLockStock(BigDecimal.ZERO);
        material.setVersion(0);
        return materialService.save(material) ? Result.success("新增成功") : Result.fail("新增失败");
    }

    @RequireRole({"admin", "warehouse"})
    @PutMapping("/update")
    public Result<String> update(@Valid @RequestBody Material material) {
        if (material.getId() == null) {
            return Result.fail("物料ID不能为空");
        }
        Material existing = materialService.getById(material.getId());
        if (existing == null) {
            return Result.fail("物料不存在");
        }
        // 安全：库存/占用/版本不允许请求体设置（库存只能通过出入库流程变更）。
        // 从数据库重新读取版本号，保证乐观锁仍生效（并发修改时 updateById 返回 false）。
        material.setStock(null);
        material.setLockStock(null);
        material.setVersion(existing.getVersion());
        return materialService.updateById(material) ? Result.success("更新成功") : Result.fail("更新失败或已被他人修改，请刷新重试");
    }

    @RequireRole("admin")
    @DeleteMapping("/del/{id}")
    public Result<String> delete(@PathVariable Long id) {
        return materialService.removeById(id) ? Result.success("删除成功") : Result.fail("删除失败");
    }
}
