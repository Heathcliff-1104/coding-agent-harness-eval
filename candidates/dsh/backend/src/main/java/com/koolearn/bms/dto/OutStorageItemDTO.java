package com.koolearn.bms.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class OutStorageItemDTO {
    private Long id;
    private Long outboundId;
    private Long materialId;
    private String materialCode;
    private String batchNo;
    private BigDecimal outNum;
}