package com.koolearn.bms.dto;

import com.koolearn.bms.entity.InStorageItem;
import lombok.Data;
import java.util.List;

@Data
public class InboundOrderDTO {
    private Long id;
    private String billNo;
    /** 1采购入库 2退库入库 */
    private Integer inType;
    /** 退库原因（退库入库时填写） */
    private String returnReason;
    private String supplier;
    private String userName;
    private String inDate;
    private String remark;
    private List<InStorageItem> itemList;
}