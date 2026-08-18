package com.koolearn.bms.dto;

import com.koolearn.bms.entity.InStorageItem;
import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

@Data
public class InboundOrderDTO {
    private String billNo;
    private String supplier;
    private String userName;
    private String inDate;
    /** 入库类型: PURCHASE=采购入库 RETURN=退库入库 */
    private String inType;
    /** 退库原因（退库入库时填写） */
    private String returnReason;
    private String remark;
    @NotNull(message = "入库明细不能为空")
    @Size(min = 1, message = "入库明细不能为空")
    private List<InStorageItem> itemList;
}