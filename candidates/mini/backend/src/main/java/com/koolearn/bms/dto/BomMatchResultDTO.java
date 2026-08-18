package com.koolearn.bms.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class BomMatchResultDTO {
    private Long materialId;
    private String materialCode;
    private String materialName;
    private String packageType;
    private String valueData;
    private String specModel;
    private String batchNo;
    private BigDecimal needNum;
    private BigDecimal currentStock;
    private BigDecimal shortage;
    private String stockStatus;   // sufficient / insufficient / occupied / out_of_stock
}
