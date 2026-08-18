package com.koolearn.bms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.koolearn.bms.dto.OutboundOrderDTO;
import com.koolearn.bms.dto.OutStorageItemDTO;
import com.koolearn.bms.entity.*;
import com.koolearn.bms.mapper.*;
import com.koolearn.bms.service.CisSyncService;
import com.koolearn.bms.service.DingTalkNotifier;
import com.koolearn.bms.service.MaterialService;
import com.koolearn.bms.service.OutboundOrderService;
import com.koolearn.bms.util.dingtalk.DingTalkApprovalUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.math.BigDecimal;

@Slf4j
@Service
public class OutboundOrderServiceImpl extends ServiceImpl<OutboundOrderMapper, OutboundOrder> implements OutboundOrderService {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final ExecutorService CIS_EXECUTOR = Executors.newSingleThreadExecutor();

    private final OutboundOrderMapper outboundOrderMapper;
    private final OutStorageItemMapper outStorageItemMapper;
    private final OutRecordMapper outRecordMapper;
    private final MaterialService materialService;
    private final DingTalkApprovalUtil dingTalkApprovalUtil;
    private final CisSyncService cisSyncService;
    private final DingTalkNotifier dingTalkNotifier;

    public OutboundOrderServiceImpl(OutboundOrderMapper outboundOrderMapper,
                                    OutStorageItemMapper outStorageItemMapper,
                                    OutRecordMapper outRecordMapper,
                                    MaterialService materialService,
                                    DingTalkApprovalUtil dingTalkApprovalUtil,
                                    CisSyncService cisSyncService,
                                    DingTalkNotifier dingTalkNotifier) {
        this.outboundOrderMapper = outboundOrderMapper;
        this.outStorageItemMapper = outStorageItemMapper;
        this.outRecordMapper = outRecordMapper;
        this.materialService = materialService;
        this.dingTalkApprovalUtil = dingTalkApprovalUtil;
        this.cisSyncService = cisSyncService;
        this.dingTalkNotifier = dingTalkNotifier;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveDraft(OutboundOrderDTO dto) {
        OutboundOrder order = new OutboundOrder();
        order.setOutboundCode("OUT" + System.currentTimeMillis());
        order.setOutType(dto.getOutType());
        order.setApplyUser(dto.getApplyUser());
        order.setRemark(dto.getRemark());
        order.setOrderStatus(0);
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        outboundOrderMapper.insert(order);

        List<OutStorageItemDTO> itemDTOList = dto.getItemList();
        List<OutStorageItem> itemList = new ArrayList<>();
        for (OutStorageItemDTO itemDTO : itemDTOList) {
            Material mat = materialService.getById(itemDTO.getMaterialId());
            if (mat == null) throw new RuntimeException("物料不存在: " + itemDTO.getMaterialId());
            BigDecimal available = mat.getStock().subtract(mat.getLockStock() != null ? mat.getLockStock() : BigDecimal.ZERO);
            if (available.compareTo(itemDTO.getOutNum()) < 0) {
                throw new RuntimeException("物料【" + mat.getMaterialName() + "】库存不足！可用:" + available + " 需要:" + itemDTO.getOutNum());
            }
            boolean locked = materialService.lockMaterialStock(itemDTO.getMaterialId(), itemDTO.getOutNum());
            if (!locked) {
                throw new RuntimeException("物料【" + mat.getMaterialName() + "】库存被并发占用，请重试");
            }
            OutStorageItem item = new OutStorageItem();
            item.setOutboundId(order.getId());
            item.setMaterialId(itemDTO.getMaterialId());
            item.setMaterialCode(itemDTO.getMaterialCode());
            item.setBatchNo(itemDTO.getBatchNo());
            item.setOutNum(itemDTO.getOutNum());
            item.setCreateTime(LocalDateTime.now());
            itemList.add(item);
        }
        for (OutStorageItem item : itemList) {
            outStorageItemMapper.insert(item);
        }
        return order.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void editDraft(Long id, OutboundOrderDTO dto) {
        OutboundOrder order = outboundOrderMapper.selectById(id);
        if (order == null || !order.getOrderStatus().equals(0)) {
            throw new RuntimeException("仅草稿状态可修改");
        }
        List<OutStorageItem> oldItemList = outStorageItemMapper.selectByOutboundId(id);
        for (OutStorageItem oldItem : oldItemList) {
            materialService.unLockMaterialStock(oldItem.getMaterialId(), oldItem.getOutNum());
        }
        order.setOutType(dto.getOutType());
        order.setApplyUser(dto.getApplyUser());
        order.setRemark(dto.getRemark());
        order.setUpdateTime(LocalDateTime.now());
        outboundOrderMapper.updateById(order);
        outStorageItemMapper.deleteByOutboundId(id);

        List<OutStorageItemDTO> itemDTOList = dto.getItemList();
        List<OutStorageItem> itemList = new ArrayList<>();
        for (OutStorageItemDTO itemDTO : itemDTOList) {
            // 重新校验可用库存（可用 = stock - lockStock，lockStock 为空按 0 处理）
            Material mat = materialService.getById(itemDTO.getMaterialId());
            if (mat == null) throw new RuntimeException("物料不存在: " + itemDTO.getMaterialId());
            BigDecimal available = mat.getStock().subtract(mat.getLockStock() != null ? mat.getLockStock() : BigDecimal.ZERO);
            if (available.compareTo(itemDTO.getOutNum()) < 0) {
                throw new RuntimeException("物料【" + mat.getMaterialName() + "】库存不足！可用:" + available + " 需要:" + itemDTO.getOutNum());
            }
            boolean locked = materialService.lockMaterialStock(itemDTO.getMaterialId(), itemDTO.getOutNum());
            if (!locked) {
                throw new RuntimeException("物料【" + mat.getMaterialName() + "】库存被并发占用，请重试");
            }
            OutStorageItem item = new OutStorageItem();
            item.setOutboundId(id);
            item.setMaterialId(itemDTO.getMaterialId());
            item.setMaterialCode(itemDTO.getMaterialCode());
            item.setBatchNo(itemDTO.getBatchNo());
            item.setOutNum(itemDTO.getOutNum());
            item.setCreateTime(LocalDateTime.now());
            itemList.add(item);
        }
        for (OutStorageItem item : itemList) {
            outStorageItemMapper.insert(item);
        }
    }

    @Override
    public OutboundOrderDTO getDetailById(Long id) {
        OutboundOrder order = outboundOrderMapper.selectById(id);
        if (order == null) return null;
        OutboundOrderDTO dto = new OutboundOrderDTO();
        dto.setId(order.getId());
        dto.setOutboundCode(order.getOutboundCode());
        dto.setOutType(order.getOutType());
        dto.setApplyUser(order.getApplyUser());
        dto.setOperUser(order.getOperUser());
        dto.setRemark(order.getRemark());
        dto.setOrderStatus(order.getOrderStatus());
        dto.setDingInstanceId(order.getDingInstanceId());

        List<OutStorageItem> itemList = outStorageItemMapper.selectByOutboundId(id);
        List<OutStorageItemDTO> itemDTOList = new ArrayList<>();
        for (OutStorageItem item : itemList) {
            OutStorageItemDTO itemDTO = new OutStorageItemDTO();
            itemDTO.setId(item.getId());
            itemDTO.setOutboundId(item.getOutboundId());
            itemDTO.setMaterialId(item.getMaterialId());
            itemDTO.setMaterialCode(item.getMaterialCode());
            itemDTO.setBatchNo(item.getBatchNo());
            itemDTO.setOutNum(item.getOutNum());
            itemDTOList.add(itemDTO);
        }
        dto.setItemList(itemDTOList);
        return dto;
    }

    @Override
    public Page<OutboundOrder> getOrderPage(Integer pageNum, Integer pageSize, String outboundCode, Integer outType, Integer orderStatus, String keyword, String applyUser) {
        Page<OutboundOrder> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<OutboundOrder> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(OutboundOrder::getOutboundCode, keyword).or().like(OutboundOrder::getApplyUser, keyword));
        }
        if (StringUtils.hasText(outboundCode)) wrapper.like(OutboundOrder::getOutboundCode, outboundCode);
        if (outType != null) wrapper.eq(OutboundOrder::getOutType, outType);
        if (orderStatus != null) wrapper.eq(OutboundOrder::getOrderStatus, orderStatus);
        // 数据范围：非 admin/warehouse 只能看到自己的申请单
        if (StringUtils.hasText(applyUser)) wrapper.eq(OutboundOrder::getApplyUser, applyUser);
        wrapper.orderByDesc(OutboundOrder::getCreateTime);
        return outboundOrderMapper.selectPage(page, wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String saveOrder(OutboundOrderDTO dto) {
        OutboundOrder order = outboundOrderMapper.selectById(dto.getId());
        if (order == null || !order.getOrderStatus().equals(0)) {
            throw new RuntimeException("单据状态异常，无法提交审批");
        }

        List<OutStorageItem> itemList = outStorageItemMapper.selectByOutboundId(order.getId());
        String detailJson;
        try {
            detailJson = MAPPER.writeValueAsString(itemList);
        } catch (Exception e) {
            throw new RuntimeException("序列化出库明细失败", e);
        }

        try {
            String dingInstanceId = dingTalkApprovalUtil.createOutboundApproval(
                    order.getOutboundCode(),
                    order.getOutType(),
                    order.getApplyUser(),
                    order.getRemark(),
                    detailJson
            );
            order.setDingInstanceId(dingInstanceId);
            order.setUpdateTime(LocalDateTime.now());
            outboundOrderMapper.updateById(order);
            return dingInstanceId;
        } catch (Exception e) {
            log.warn("钉钉出库审批发起失败，订单已保存: {}", e.getMessage());
            return null;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmOut(Long outboundId, String operUser) {
        OutboundOrder order = outboundOrderMapper.selectById(outboundId);
        if (order == null) throw new RuntimeException("出库单不存在");
        // 原子状态守卫（第一步）：0 -> 1，防止 TOCTOU（两请求同时确认）。
        // 若 0 行受影响说明状态已变化（已确认/已驳回），抛异常由事务回滚。
        if (outboundOrderMapper.atomicConfirm(outboundId) == 0) {
            throw new RuntimeException("单据状态异常或已处理");
        }

        List<OutStorageItem> itemList = outStorageItemMapper.selectByOutboundId(outboundId);

        for (OutStorageItem item : itemList) {
            Material mat = materialService.getById(item.getMaterialId());
            if (mat == null) throw new RuntimeException("物料ID " + item.getMaterialId() + " 不存在");
            // 可用校验只看总库存：lockStock 已包含本单自己的占用，
            // 若用 stock-lockStock 判断，多单共同预订同一物料时永远无法确认。
            // 真正的并发保护是 atomicSubStock 的 WHERE stock >= num。
            if (mat.getStock() == null || mat.getStock().compareTo(item.getOutNum()) < 0) {
                throw new RuntimeException("物料【" + mat.getMaterialName() + "】库存不足！库存:" + mat.getStock() + " 需要:" + item.getOutNum());
            }
        }

        for (OutStorageItem item : itemList) {
            materialService.decreaseMaterialStock(item.getMaterialId(), item.getOutNum());
            materialService.unLockMaterialStock(item.getMaterialId(), item.getOutNum());
        }
        for (OutStorageItem item : itemList) {
            OutRecord record = new OutRecord();
            record.setOutboundCode(order.getOutboundCode());
            record.setMaterialId(item.getMaterialId());
            record.setBatchNo(item.getBatchNo());
            record.setOutNum(item.getOutNum());
            record.setOutUser(operUser);
            record.setOutTime(LocalDateTime.now());
            outRecordMapper.insert(record);
        }
        order.setOrderStatus(1);
        order.setOperUser(operUser);
        outboundOrderMapper.updateById(order);
        fireCisSync();
    }

    private void fireCisSync() {
        try {
            CIS_EXECUTOR.submit(() -> {
                try {
                    cisSyncService.syncIncremental();
                } catch (Exception e) {
                    log.warn("CIS增量同步失败: {}", e.getMessage());
                }
            });
        } catch (Exception e) {
            log.warn("CIS同步提交异常: {}", e.getMessage());
        }
    }

    @Override
    public OutboundOrder getByDingInstanceId(String instanceId) {
        return outboundOrderMapper.selectByDingInstanceId(instanceId);
    }

    @Override
    public boolean approveFromCallback(Long id) {
        return outboundOrderMapper.approveFromCallback(id) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectOut(Long id) {
        OutboundOrder order = outboundOrderMapper.selectById(id);
        if (order == null) throw new RuntimeException("单据不存在");
        // 原子状态守卫：0 -> 2，防止 TOCTOU
        if (outboundOrderMapper.atomicReject(id) == 0) {
            throw new RuntimeException("单据状态异常，无法驳回");
        }
        List<OutStorageItem> itemList = outStorageItemMapper.selectByOutboundId(id);
        for (OutStorageItem item : itemList) {
            materialService.unLockMaterialStock(item.getMaterialId(), item.getOutNum());
        }
        order.setOrderStatus(2);
        order.setUpdateTime(LocalDateTime.now());
        outboundOrderMapper.updateById(order);
        try {
            dingTalkNotifier.send("出库单被驳回", "出库单【" + order.getOutboundCode() + "】已被驳回，库存占用已释放",
                    Arrays.asList(order.getApplyUser()));
        } catch (Exception e) {
            log.warn("驳回通知失败: {}", e.getMessage());
        }
    }
}
