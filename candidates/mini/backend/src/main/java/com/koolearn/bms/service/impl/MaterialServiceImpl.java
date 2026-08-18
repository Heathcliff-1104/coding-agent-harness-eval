package com.koolearn.bms.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.koolearn.bms.entity.Material;
import com.koolearn.bms.mapper.MaterialMapper;
import com.koolearn.bms.service.MaterialService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class MaterialServiceImpl extends ServiceImpl<MaterialMapper, Material> implements MaterialService {

    @Override
    public Material getById(Long id) {
        return super.getById(id);
    }

    @Override
    public Material getByCode(String materialCode) {
        if (materialCode == null || materialCode.trim().isEmpty()) return null;
        return getOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Material>()
                .eq(Material::getMaterialCode, materialCode.trim()));
    }

    @Override
    public List<Material> listAll() {
        return super.list();
    }

    @Override
    public IPage<Material> pageQuery(Long pageNum, Long pageSize, String code, String name, String warehouse, String keyword) {
        return pageQuery(pageNum, pageSize, code, name, warehouse, null, keyword);
    }

    @Override
    public IPage<Material> pageQuery(Long pageNum, Long pageSize, String code, String name, String warehouse, String packageType, String keyword) {
        Page<Material> page = new Page<>(pageNum, pageSize);
        return baseMapper.selectPageByCondition(page, code, name, warehouse, packageType, keyword);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addStock(Long materialId, BigDecimal addNum) {
        return retryWithBackoff(() -> {
            Material mat = getById(materialId);
            if (mat == null) return false;
            return baseMapper.atomicAddStock(materialId, addNum, mat.getVersion()) > 0;
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean subStock(Long materialId, BigDecimal subNum) {
        return retryWithBackoff(() -> {
            Material mat = getById(materialId);
            if (mat == null || mat.getStock().compareTo(subNum) < 0) return false;
            return baseMapper.atomicSubStock(materialId, subNum, mat.getVersion()) > 0;
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean lockMaterialStock(Long materialId, BigDecimal num) {
        return retryWithBackoff(() -> {
            Material mat = getById(materialId);
            if (mat == null) return false;
            return baseMapper.lockStock(materialId, num, mat.getVersion()) > 0;
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean unLockMaterialStock(Long materialId, BigDecimal num) {
        return retryWithBackoff(() -> {
            Material mat = getById(materialId);
            if (mat == null || mat.getLockStock().compareTo(num) < 0) return false;
            return baseMapper.unLockStock(materialId, num, mat.getVersion()) > 0;
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
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
