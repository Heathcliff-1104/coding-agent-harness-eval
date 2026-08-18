package com.koolearn.bms.controller;

import com.koolearn.bms.annotation.RequireRole;
import com.koolearn.bms.dto.BomImportDTO;
import com.koolearn.bms.dto.BomItemDTO;
import com.koolearn.bms.dto.BomMatchResultDTO;
import com.koolearn.bms.dto.BomSavePlanDTO;
import com.koolearn.bms.service.BomService;
import com.koolearn.bms.util.Result;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/outbound/bom")
public class BomController {

    private final BomService bomService;

    public BomController(BomService bomService) {
        this.bomService = bomService;
    }

    @RequireRole({"admin", "warehouse", "engineer"})
    @PostMapping("/match")
    public Result<List<BomMatchResultDTO>> match(@RequestBody List<BomItemDTO> items) {
        return Result.success(bomService.match(items));
    }

    @RequireRole({"admin", "warehouse", "engineer"})
    @PostMapping("/import")
    public Result<Map<String, Object>> importBom(@RequestBody BomImportDTO body,
                                                 @RequestAttribute("username") String username) {
        return Result.success(bomService.importBom(body.getBomName(), username, body.getItems()));
    }

    @RequireRole({"admin", "warehouse", "engineer"})
    @PostMapping("/savePlan")
    public Result<String> savePlan(@RequestBody BomSavePlanDTO body,
                                   @RequestAttribute("username") String username) {
        return Result.success(bomService.savePlan(body.getBomId(), body.getPlanNo(), username, body.getItems(), body.getRemark()));
    }

    @GetMapping("/versions")
    public Result<List<Map<String, Object>>> versions() {
        return Result.success(bomService.listVersions());
    }
}
