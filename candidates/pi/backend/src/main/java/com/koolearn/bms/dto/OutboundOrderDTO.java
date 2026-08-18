package com.koolearn.bms.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

@Data
public class OutboundOrderDTO {
    private Long id;
    private String outboundCode;
    private Integer outType;
    private String applyUser;
    private String operUser;
    private String remark;
    private Integer orderStatus;
    private String dingInstanceId;
    // 出库明细列表
    @NotNull(message = "出库明细不能为空")
    @Size(min = 1, message = "出库明细不能为空")
    private List<OutStorageItemDTO> itemList;
}