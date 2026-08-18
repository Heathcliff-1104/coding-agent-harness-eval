package com.koolearn.bms.service;

import com.koolearn.bms.dto.BomItemDTO;
import com.koolearn.bms.dto.BomMatchResultDTO;

import java.util.List;
import java.util.Map;

public interface BomService {
    List<BomMatchResultDTO> match(List<BomItemDTO> items);
    Map<String, Object> importBom(String bomName, String creator, List<BomItemDTO> items);
    String savePlan(Long bomId, String planNo, String creator, List<BomItemDTO> items, String remark);
    List<Map<String, Object>> listVersions();
}
