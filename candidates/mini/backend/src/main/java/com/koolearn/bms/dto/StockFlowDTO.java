package com.koolearn.bms.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class StockFlowDTO {
    private String recordType;   // in / out / return
    private Long materialId;
    private String materialCode;
    private String materialName;
    private String billNo;
    private String batchNo;
    private BigDecimal num;
    private String operator;
    private LocalDateTime opTime;
}
