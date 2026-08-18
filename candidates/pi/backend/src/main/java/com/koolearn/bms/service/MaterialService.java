package com.koolearn.bms.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.koolearn.bms.entity.Material;
import java.math.BigDecimal;
import java.util.List;

public interface MaterialService extends IService<Material> {
    Material getById(Long id);
    List<Material> listAll();
    IPage<Material> pageQuery(Long pageNum, Long pageSize, String code, String name, String warehouse, String keyword);

    boolean addStock(Long materialId, BigDecimal addNum);
    boolean subStock(Long materialId, BigDecimal subNum);

    boolean lockMaterialStock(Long materialId, BigDecimal num);
    boolean unLockMaterialStock(Long materialId, BigDecimal num);

    void decreaseMaterialStock(Long materialId, BigDecimal num);

    BigDecimal getMaterialStock(Long materialId);
}
