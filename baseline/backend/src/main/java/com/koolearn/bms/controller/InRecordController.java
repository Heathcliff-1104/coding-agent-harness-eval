package com.koolearn.bms.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.koolearn.bms.entity.InRecord;
import com.koolearn.bms.service.InRecordService;
import com.koolearn.bms.util.Result;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/inRecord")
public class InRecordController {

    private final InRecordService inRecordService;

    public InRecordController(InRecordService inRecordService) {
        this.inRecordService = inRecordService;
    }

    @GetMapping("/list")
    public Result<List<InRecord>> list(@RequestParam String startDate, @RequestParam String endDate) {
        return Result.success(inRecordService.selectByDate(startDate, endDate));
    }

    @GetMapping("/page")
    public Result<IPage<InRecord>> page(@RequestParam(defaultValue = "1") Long pageNum,
                                        @RequestParam(defaultValue = "10") Long pageSize,
                                        @RequestParam(required = false) String startDate,
                                        @RequestParam(required = false) String endDate,
                                        @RequestParam(required = false) String keyword,
                                        @RequestParam(required = false) String billNo) {
        return Result.success(inRecordService.pageQuery(pageNum, pageSize, startDate, endDate, keyword, billNo));
    }
}
