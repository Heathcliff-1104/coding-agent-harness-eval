package com.koolearn.bms.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.koolearn.bms.entity.Material;
import com.koolearn.bms.mapper.MaterialMapper;
import com.koolearn.bms.service.MaterialService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class MaterialServiceImpl extends ServiceImpl<MaterialMapper, Material> implements MaterialService {

    /** 重试助手：每次尝试用锁定读(当前读)读取最新版本（见 MaterialStockRetryHelper 注释） */
    private final MaterialStockRetryHelper stockRetryHelper;

    public MaterialServiceImpl(MaterialStockRetryHelper stockRetryHelper) {
        this.stockRetryHelper = stockRetryHelper;
    }

    @Override
    public Material getById(Long id) {
        return super.getById(id);
    }

    @Override
    public List<Material> listAll() {
        return super.list();
    }

    @Override
    public IPage<Material> pageQuery(Long pageNum, Long pageSize, String code, String name, String warehouse, String keyword) {
        Page<Material> page = new Page<>(pageNum, pageSize);
        return baseMapper.selectPageByCondition(page, code, name, warehouse, keyword);
    }

    @Override
    public boolean addStock(Long materialId, BigDecimal addNum) {
        return retryWithBackoff(() -> stockRetryHelper.addStockAttempt(materialId, addNum));
    }

    @Override
    public boolean subStock(Long materialId, BigDecimal subNum) {
        return retryWithBackoff(() -> stockRetryHelper.subStockAttempt(materialId, subNum));
    }

    @Override
    public boolean lockMaterialStock(Long materialId, BigDecimal num) {
        return retryWithBackoff(() -> stockRetryHelper.lockStockAttempt(materialId, num));
    }

    @Override
    public boolean unLockMaterialStock(Long materialId, BigDecimal num) {
        return retryWithBackoff(() -> stockRetryHelper.unLockStockAttempt(materialId, num));
    }

    @Override
    public void decreaseMaterialStock(Long materialId, BigDecimal num) {
        boolean success = subStock(materialId, num);
        if (!success) {
            throw new RuntimeException("物料可用库存不足，无法出库");
        }
    }

    @Override
    public BigDecimal getMaterialStock(Long materialId) {
        Material material = getById(materialId);
        return material == null ? BigDecimal.ZERO : material.getStock();
    }

    private boolean retryWithBackoff(RetrySupplier supplier) {
        int retry = 3;
        int delayMs = 50;
        while (retry > 0) {
            try {
                if (supplier.tryUpdate()) return true;
            } catch (RuntimeException e) {
                throw e;
            }
            retry--;
            if (retry > 0) {
                try { Thread.sleep(delayMs); } catch (InterruptedException e) { Thread.currentThread().interrupt(); break; }
                delayMs *= 2;
            }
        }
        throw new RuntimeException("库存并发冲突，请重试");
    }

    @FunctionalInterface
    private interface RetrySupplier {
        boolean tryUpdate();
    }
}
