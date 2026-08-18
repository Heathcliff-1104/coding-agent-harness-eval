package com.koolearn.bms.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.koolearn.bms.entity.OutRecord;
import com.koolearn.bms.service.OutRecordService;
import com.koolearn.bms.util.Result;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/outRecord")
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
            @RequestParam(required = false) String endTime) {
        Page<OutRecord> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<OutRecord> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(outboundCode)) wrapper.like(OutRecord::getOutboundCode, outboundCode);
        if (materialId != null) wrapper.eq(OutRecord::getMaterialId, materialId);
        if (StringUtils.hasText(startTime)) wrapper.ge(OutRecord::getOutTime, startTime);
        if (StringUtils.hasText(endTime)) wrapper.le(OutRecord::getOutTime, endTime);
        wrapper.orderByDesc(OutRecord::getOutTime);
        return Result.success(outRecordService.page(page, wrapper));
    }
}
