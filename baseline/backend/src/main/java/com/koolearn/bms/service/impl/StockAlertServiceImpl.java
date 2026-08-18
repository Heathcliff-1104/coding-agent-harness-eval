package com.koolearn.bms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.koolearn.bms.entity.Material;
import com.koolearn.bms.entity.StockAlert;
import com.koolearn.bms.mapper.MaterialMapper;
import com.koolearn.bms.mapper.StockAlertMapper;
import com.koolearn.bms.service.StockAlertService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Slf4j
@Service
public class StockAlertServiceImpl extends ServiceImpl<StockAlertMapper, StockAlert> implements StockAlertService {

    private final MaterialMapper materialMapper;

    public StockAlertServiceImpl(MaterialMapper materialMapper) {
        this.materialMapper = materialMapper;
    }

    @Override
    public void scanAndAlert() {
        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = LocalDateTime.of(today, LocalTime.MIN);

        for (Material m : materialMapper.selectList(null)) {
            if (m.getMinStock() != null && m.getStock().compareTo(m.getMinStock()) < 0) {
                if (!alertExistsToday(m.getId(), 1, todayStart)) {
                    createAlert(m, 1, m.getMinStock());
                }
            }
            if (m.getMaxStock() != null && m.getStock().compareTo(m.getMaxStock()) > 0) {
                if (!alertExistsToday(m.getId(), 2, todayStart)) {
                    createAlert(m, 2, m.getMaxStock());
                }
            }
        }
    }

    private boolean alertExistsToday(Long materialId, Integer alertType, LocalDateTime todayStart) {
        return baseMapper.selectCount(new LambdaQueryWrapper<StockAlert>()
                .eq(StockAlert::getMaterialId, materialId)
                .eq(StockAlert::getAlertType, alertType)
                .eq(StockAlert::getHandled, 0)
                .ge(StockAlert::getCreateTime, todayStart)) > 0;
    }

    private void createAlert(Material m, Integer alertType, java.math.BigDecimal threshold) {
        StockAlert alert = new StockAlert();
        alert.setMaterialId(m.getId());
        alert.setMaterialCode(m.getMaterialCode());
        alert.setMaterialName(m.getMaterialName());
        alert.setAlertType(alertType);
        alert.setCurrentStock(m.getStock());
        alert.setThresholdStock(threshold);
        alert.setHandled(0);
        alert.setCreateTime(LocalDateTime.now());
        baseMapper.insert(alert);
        if (alertType == 1) {
            log.info("低库存预警: {} 当前{} < 最低{}", m.getMaterialName(), m.getStock(), threshold);
        } else {
            log.info("超储预警: {} 当前{} > 最高{}", m.getMaterialName(), m.getStock(), threshold);
        }
    }
}
