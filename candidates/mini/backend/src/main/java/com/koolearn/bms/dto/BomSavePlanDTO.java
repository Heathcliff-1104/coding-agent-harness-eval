package com.koolearn.bms.dto;

import lombok.Data;
import java.util.List;

@Data
public class BomSavePlanDTO {
    private Long bomId;
    private String planNo;
    private String remark;
    private List<BomItemDTO> items;
}
