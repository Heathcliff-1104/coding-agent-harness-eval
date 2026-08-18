package com.koolearn.bms.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.koolearn.bms.annotation.RequireRole;
import com.koolearn.bms.entity.InRecord;
import com.koolearn.bms.service.InRecordService;
import com.koolearn.bms.util.ExcelExportUtil;
import com.koolearn.bms.util.Result;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/inRecord")
@RequireRole({"admin", "warehouse"})
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

    @GetMapping("/export")
    public void export(@RequestParam(required = false) String startDate,
                       @RequestParam(required = false) String endDate,
                       @RequestParam(required = false) String keyword,
                       @RequestParam(required = false) String billNo,
                       HttpServletResponse response) throws Exception {
        List<InRecord> list = inRecordService.pageQuery(1L, 100000L, startDate, endDate, keyword, billNo).getRecords();
        List<Map<String, Object>> rows = new ArrayList<>();
        for (InRecord r : list) {
            Map<String, Object> m = new HashMap<>();
            m.put("billNo", r.getBillNo());
            m.put("inTime", r.getInTime());
            m.put("inUser", r.getInUser());
            m.put("materialCode", r.getMaterialCode());
            m.put("materialName", r.getMaterialName());
            m.put("batchNo", r.getBatchNo());
            m.put("inNum", r.getInNum());
            m.put("locationNo", r.getLocationNo());
            rows.add(m);
        }
        ExcelExportUtil.export(response, "入库记录", new String[]{"入库单号", "入库时间", "操作人", "物料编码", "物料名称", "批次号", "数量", "存放货位"},
                rows, new String[]{"billNo", "inTime", "inUser", "materialCode", "materialName", "batchNo", "inNum", "locationNo"});
    }
}
