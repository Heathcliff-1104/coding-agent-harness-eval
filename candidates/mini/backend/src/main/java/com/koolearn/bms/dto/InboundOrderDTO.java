package com.koolearn.bms.dto;

import com.koolearn.bms.entity.InStorageItem;
import lombok.Data;
import java.util.List;

@Data
public class InboundOrderDTO {
    private String billNo;
    private String supplier;
    private String userName;
    private String inDate;
    private String remark;
    private List<InStorageItem> itemList;
}