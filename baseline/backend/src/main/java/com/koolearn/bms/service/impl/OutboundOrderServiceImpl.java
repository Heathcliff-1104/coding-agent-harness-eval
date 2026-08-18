package com.koolearn.bms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.koolearn.bms.dto.OutboundOrderDTO;
import com.koolearn.bms.dto.OutStorageItemDTO;
import com.koolearn.bms.entity.*;
import com.koolearn.bms.mapper.*;
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
import java.util.List;
import java.math.BigDecimal;

@Slf4j
@Service
public class OutboundOrderServiceImpl extends ServiceImpl<OutboundOrderMapper, OutboundOrder> implements OutboundOrderService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final OutboundOrderMapper outboundOrderMapper;
    private final OutStorageItemMapper outStorageItemMapper;
    private final OutRecordMapper outRecordMapper;
    private final MaterialService materialService;
    private final DingTalkApprovalUtil dingTalkApprovalUtil;

    public OutboundOrderServiceImpl(OutboundOrderMapper outboundOrderMapper,
                                    OutStorageItemMapper outStorageItemMapper,
                                    OutRecordMapper outRecordMapper,
                                    MaterialService materialService,
                                    DingTalkApprovalUtil dingTalkApprovalUtil) {
        this.outboundOrderMapper = outboundOrderMapper;
        this.outStorageItemMapper = outStorageItemMapper;
        this.outRecordMapper = outRecordMapper;
        this.materialService = materialService;
        this.dingTalkApprovalUtil = dingTalkApprovalUtil;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveDraft(OutboundOrderDTO dto) {
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
            materialService.lockMaterialStock(itemDTO.getMaterialId(), itemDTO.getOutNum());
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
            materialService.lockMaterialStock(itemDTO.getMaterialId(), itemDTO.getOutNum());
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
    public Page<OutboundOrder> getOrderPage(Integer pageNum, Integer pageSize, String outboundCode, Integer outType, Integer orderStatus, String keyword) {
        Page<OutboundOrder> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<OutboundOrder> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(OutboundOrder::getOutboundCode, keyword).or().like(OutboundOrder::getApplyUser, keyword));
        }
        if (StringUtils.hasText(outboundCode)) wrapper.like(OutboundOrder::getOutboundCode, outboundCode);
        if (outType != null) wrapper.eq(OutboundOrder::getOutType, outType);
        if (orderStatus != null) wrapper.eq(OutboundOrder::getOrderStatus, orderStatus);
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
        if (!order.getOrderStatus().equals(0)) throw new RuntimeException("仅待审批单据可执行出库");

        List<OutStorageItem> itemList = outStorageItemMapper.selectByOutboundId(outboundId);

        for (OutStorageItem item : itemList) {
            Material mat = materialService.getById(item.getMaterialId());
            if (mat == null) throw new RuntimeException("物料ID " + item.getMaterialId() + " 不存在");
            BigDecimal available = mat.getStock().subtract(mat.getLockStock() != null ? mat.getLockStock() : BigDecimal.ZERO);
            if (available.compareTo(item.getOutNum()) < 0) {
                throw new RuntimeException("物料【" + mat.getMaterialName() + "】库存不足！可用:" + available + " 需要:" + item.getOutNum());
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
    }

    @Override
    public OutboundOrder getByDingInstanceId(String instanceId) {
        return outboundOrderMapper.selectByDingInstanceId(instanceId);
    }

    @Override
    public boolean updateStatus(Long id, Integer status) {
        return outboundOrderMapper.updateOrderStatus(id, status) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectOut(Long id) {
        OutboundOrder order = outboundOrderMapper.selectById(id);
        if (order == null || !order.getOrderStatus().equals(0)) {
            throw new RuntimeException("单据状态异常，无法驳回");
        }
        List<OutStorageItem> itemList = outStorageItemMapper.selectByOutboundId(id);
        for (OutStorageItem item : itemList) {
            materialService.unLockMaterialStock(item.getMaterialId(), item.getOutNum());
        }
        order.setOrderStatus(2);
        order.setUpdateTime(LocalDateTime.now());
        outboundOrderMapper.updateById(order);
    }
}
