package com.koolearn.bms.controller;

import com.koolearn.bms.entity.InStorageItem;
import com.koolearn.bms.service.InStorageItemService;
import com.koolearn.bms.util.Result;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/inItem")
public class InItemController {

    private final InStorageItemService itemService;

    public InItemController(InStorageItemService itemService) {
        this.itemService = itemService;
    }

    @GetMapping("/list/{inboundId}")
    public Result<List<InStorageItem>> getItemList(@PathVariable Long inboundId) {
        return Result.success(itemService.selectByInboundId(inboundId));
    }
}
