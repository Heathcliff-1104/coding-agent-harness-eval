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
    public IPage<Material> pageQuery(Long pageNum, Long pageSize, String code, String name, String warehouse, String keyword, String packageType) {
        Page<Material> page = new Page<>(pageNum, pageSize);
        IPage<Material> result = baseMapper.selectPageByCondition(page, code, name, warehouse, keyword, packageType);
        result.getRecords().forEach(MaterialServiceImpl::fillStatus);
        return result;
    }

    @Override
    public List<Material> listAll() {
        List<Material> list = super.list();
        list.forEach(MaterialServiceImpl::fillStatus);
        return list;
    }

    @Override
    public Material getById(Long id) {
        Material m = super.getById(id);
        if (m != null) fillStatus(m);
        return m;
    }

    /** 计算物料状态：占用(出库中)/空闲/缺货 */
    private static void fillStatus(Material m) {
        if (m.getStock() == null) m.setStock(java.math.BigDecimal.ZERO);
        if (m.getLockStock() == null) m.setLockStock(java.math.BigDecimal.ZERO);
        if (m.getLockStock().compareTo(java.math.BigDecimal.ZERO) > 0) {
            m.setStatus("出库中");
        } else if (m.getStock().compareTo(java.math.BigDecimal.ZERO) > 0) {
            m.setStatus("空闲");
        } else {
            m.setStatus("缺货");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addStock(Long materialId, BigDecimal addNum) {
        return retryWithBackoff(() -> {
            Material mat = getById(materialId);
            if (mat == null) return false;
            if (addNum == null || addNum.compareTo(java.math.BigDecimal.ZERO) <= 0) return false;
            return baseMapper.atomicAddStock(materialId, addNum, ver(mat)) > 0;
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean subStock(Long materialId, BigDecimal subNum) {
        return retryWithBackoff(() -> {
            Material mat = getById(materialId);
            if (mat == null || mat.getStock() == null || mat.getStock().compareTo(subNum) < 0) return false;
            if (subNum == null || subNum.compareTo(java.math.BigDecimal.ZERO) <= 0) return false;
            return baseMapper.atomicSubStock(materialId, subNum, ver(mat)) > 0;
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean lockMaterialStock(Long materialId, BigDecimal num) {
        return retryWithBackoff(() -> {
            Material mat = getById(materialId);
            if (mat == null) return false;
            if (num == null || num.compareTo(java.math.BigDecimal.ZERO) <= 0) return false;
            return baseMapper.lockStock(materialId, num, ver(mat)) > 0;
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean unLockMaterialStock(Long materialId, BigDecimal num) {
        return retryWithBackoff(() -> {
            Material mat = getById(materialId);
            if (mat == null || mat.getLockStock() == null || mat.getLockStock().compareTo(num) < 0) return false;
            if (num == null || num.compareTo(java.math.BigDecimal.ZERO) <= 0) return false;
            return baseMapper.unLockStock(materialId, num, ver(mat)) > 0;
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

    /** 乐观锁版本号兜底：老数据 version 为 NULL 时按 0 处理 */
    private int ver(Material m) {
        return m.getVersion() == null ? 0 : m.getVersion();
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
