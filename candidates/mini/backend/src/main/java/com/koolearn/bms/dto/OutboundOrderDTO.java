package com.koolearn.bms.dto;

import lombok.Data;
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
    private List<OutStorageItemDTO> itemList;
}