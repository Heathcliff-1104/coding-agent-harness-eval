package com.koolearn.bms.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class BomItemDTO {
    private Long materialId;
    private String materialCode;
    private String materialName;
    private String packageType;
    private String valueData;
    private String specModel;
    private String batchNo;
    private BigDecimal needNum;
}
