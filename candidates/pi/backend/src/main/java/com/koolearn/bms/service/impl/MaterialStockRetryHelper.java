package com.koolearn.bms.service.impl;

import com.koolearn.bms.entity.Material;
import com.koolearn.bms.mapper.MaterialMapper;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 库存操作"单次尝试"助手：每次尝试 = 锁定读最新版本 + 原子更新(乐观锁)。
 *
 * 为什么用锁定读（SELECT ... FOR UPDATE / 当前读）而不是普通 SELECT：
 * MySQL 默认隔离级别 REPEATABLE READ 下，同一事务内多次普通 SELECT 共享同一个快照。
 * 若"读取版本 + 原子 UPDATE(WHERE version=?)"复用快照，版本冲突后的重试会一直读到
 * 旧 version，UPDATE 永远 0 行，重试形同虚设。FOR UPDATE 是"当前读"，总是读取最新
 * 已提交数据（并持有行锁串行化并发库存操作），因此每次重试都能拿到最新版本。
 *
 * 说明（与 REQUIRES_NEW 方案的区别）：最初方案为每次尝试开启独立事务(REQUIRES_NEW)，
 * 但独立事务看不到调用方事务中尚未提交的新建物料行（例如测试/入库流程先插入物料再加库存），
 * 会导致"物料不存在"误判；且独立提交会逃逸外层事务的回滚范围。锁定读方案在同一事务内
 * 即解决快照陈旧问题，无上述副作用。
 */
@Component
public class MaterialStockRetryHelper {

    private final MaterialMapper materialMapper;

    public MaterialStockRetryHelper(MaterialMapper materialMapper) {
        this.materialMapper = materialMapper;
    }

    public boolean addStockAttempt(Long materialId, BigDecimal addNum) {
        Material mat = materialMapper.selectByIdForUpdate(materialId);
        if (mat == null) return false;
        return materialMapper.atomicAddStock(materialId, addNum, mat.getVersion()) > 0;
    }

    public boolean subStockAttempt(Long materialId, BigDecimal subNum) {
        Material mat = materialMapper.selectByIdForUpdate(materialId);
        if (mat == null || mat.getStock().compareTo(subNum) < 0) return false;
        return materialMapper.atomicSubStock(materialId, subNum, mat.getVersion()) > 0;
    }

    public boolean lockStockAttempt(Long materialId, BigDecimal num) {
        Material mat = materialMapper.selectByIdForUpdate(materialId);
        if (mat == null) return false;
        return materialMapper.lockStock(materialId, num, mat.getVersion()) > 0;
    }

    public boolean unLockStockAttempt(Long materialId, BigDecimal num) {
        Material mat = materialMapper.selectByIdForUpdate(materialId);
        if (mat == null || mat.getLockStock().compareTo(num) < 0) return false;
        return materialMapper.unLockStock(materialId, num, mat.getVersion()) > 0;
    }
}
