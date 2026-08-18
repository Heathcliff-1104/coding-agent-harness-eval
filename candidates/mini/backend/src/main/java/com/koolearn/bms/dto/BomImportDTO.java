package com.koolearn.bms.dto;

import lombok.Data;
import java.util.List;

@Data
public class BomImportDTO {
    private String bomName;
    private List<BomItemDTO> items;
}
