package com.koolearn.bms.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.koolearn.bms.dto.StockFlowDTO;
import com.koolearn.bms.entity.InRecord;
import com.koolearn.bms.entity.OutRecord;
import com.koolearn.bms.service.InRecordService;
import com.koolearn.bms.service.OutRecordService;
import com.koolearn.bms.util.Result;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/stockFlow")
public class StockFlowController {

    private final InRecordService inRecordService;
    private final OutRecordService outRecordService;

    public StockFlowController(InRecordService inRecordService, OutRecordService outRecordService) {
        this.inRecordService = inRecordService;
        this.outRecordService = outRecordService;
    }

    @GetMapping("/page")
    public Result<Map<String, Object>> page(@RequestParam(defaultValue = "1") Long pageNum,
                                            @RequestParam(defaultValue = "10") Long pageSize,
                                            @RequestParam(required = false) String keyword) {
        List<StockFlowDTO> all = new ArrayList<>();

        LambdaQueryWrapper<InRecord> inQw = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            inQw.and(w -> w.like(InRecord::getBillNo, keyword).or().like(InRecord::getBatchNo, keyword));
        }
        for (InRecord r : inRecordService.list(inQw)) {
            StockFlowDTO dto = new StockFlowDTO();
            dto.setRecordType("in");
            dto.setMaterialId(r.getMaterialId());
            dto.setBillNo(r.getBillNo());
            dto.setBatchNo(r.getBatchNo());
            dto.setNum(r.getInNum());
            dto.setOperator(r.getInUser());
            dto.setOpTime(r.getInTime());
            all.add(dto);
        }

        LambdaQueryWrapper<OutRecord> outQw = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            outQw.and(w -> w.like(OutRecord::getOutboundCode, keyword).or().like(OutRecord::getBatchNo, keyword));
        }
        for (OutRecord r : outRecordService.list(outQw)) {
            StockFlowDTO dto = new StockFlowDTO();
            dto.setRecordType("out");
            dto.setMaterialId(r.getMaterialId());
            dto.setBillNo(r.getOutboundCode());
            dto.setBatchNo(r.getBatchNo());
            dto.setNum(r.getOutNum());
            dto.setOperator(r.getOutUser());
            dto.setOpTime(r.getOutTime());
            all.add(dto);
        }

        all.sort((a, b) -> b.getOpTime().compareTo(a.getOpTime()));

        long total = all.size();
        long from = (pageNum - 1) * pageSize;
        long to = Math.min(from + pageSize, total);
        List<StockFlowDTO> pageData = from < total ? all.subList((int) from, (int) to) : Collections.emptyList();

        Map<String, Object> result = new HashMap<>();
        result.put("records", pageData);
        result.put("total", total);
        return Result.success(result);
    }
}
