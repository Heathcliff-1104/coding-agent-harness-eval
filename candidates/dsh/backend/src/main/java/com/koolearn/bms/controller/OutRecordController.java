package com.koolearn.bms.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.koolearn.bms.annotation.RequireRole;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.koolearn.bms.entity.OutRecord;
import com.koolearn.bms.service.OutRecordService;
import com.koolearn.bms.util.ExcelExportUtil;
import com.koolearn.bms.util.Result;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/outRecord")
@RequireRole({"admin", "warehouse"})
public class OutRecordController {

    private final OutRecordService outRecordService;

    public OutRecordController(OutRecordService outRecordService) {
        this.outRecordService = outRecordService;
    }

    @GetMapping("/page")
    public Result<IPage<OutRecord>> pageList(
            @RequestParam(defaultValue = "1") Long pageNum,
            @RequestParam(defaultValue = "10") Long pageSize,
            @RequestParam(required = false) String outboundCode,
            @RequestParam(required = false) Long materialId,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(required = false) String keyword) {
        Page<OutRecord> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<OutRecord> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(outboundCode)) wrapper.like(OutRecord::getOutboundCode, outboundCode);
        if (materialId != null) wrapper.eq(OutRecord::getMaterialId, materialId);
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(OutRecord::getMaterialCode, keyword)
                    .or().like(OutRecord::getMaterialName, keyword)
                    .or().like(OutRecord::getBatchNo, keyword));
        }
        if (StringUtils.hasText(startTime)) wrapper.ge(OutRecord::getOutTime, startTime);
        if (StringUtils.hasText(endTime)) wrapper.le(OutRecord::getOutTime, endTime);
        wrapper.orderByDesc(OutRecord::getOutTime);
        return Result.success(outRecordService.page(page, wrapper));
    }

    @GetMapping("/export")
    public void export(@RequestParam(required = false) String outboundCode,
                       @RequestParam(required = false) Long materialId,
                       @RequestParam(required = false) String startTime,
                       @RequestParam(required = false) String endTime,
                       HttpServletResponse response) throws Exception {
        Page<OutRecord> page = new Page<>(1, 100000);
        LambdaQueryWrapper<OutRecord> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(outboundCode)) wrapper.like(OutRecord::getOutboundCode, outboundCode);
        if (materialId != null) wrapper.eq(OutRecord::getMaterialId, materialId);
        if (StringUtils.hasText(startTime)) wrapper.ge(OutRecord::getOutTime, startTime);
        if (StringUtils.hasText(endTime)) wrapper.le(OutRecord::getOutTime, endTime);
        wrapper.orderByDesc(OutRecord::getOutTime);
        List<OutRecord> list = outRecordService.page(page, wrapper).getRecords();
        List<Map<String, Object>> rows = new ArrayList<>();
        for (OutRecord r : list) {
            Map<String, Object> m = new HashMap<>();
            m.put("outboundCode", r.getOutboundCode());
            m.put("outTime", r.getOutTime());
            m.put("outUser", r.getOutUser());
            m.put("materialCode", r.getMaterialCode());
            m.put("materialName", r.getMaterialName());
            m.put("batchNo", r.getBatchNo());
            m.put("outNum", r.getOutNum());
            m.put("dept", r.getDept());
            rows.add(m);
        }
        ExcelExportUtil.export(response, "出库记录", new String[]{"出库单号", "出库时间", "操作人", "物料编码", "物料名称", "批次号", "数量", "领料部门"},
                rows, new String[]{"outboundCode", "outTime", "outUser", "materialCode", "materialName", "batchNo", "outNum", "dept"});
    }
}
