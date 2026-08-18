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
import com.koolearn.bms.util.dingtalk.DingTalkNotifier;
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

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
            .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private final OutboundOrderMapper outboundOrderMapper;
    private final OutStorageItemMapper outStorageItemMapper;
    private final OutRecordMapper outRecordMapper;
    private final MaterialService materialService;
    private final DingTalkApprovalUtil dingTalkApprovalUtil;
    private final UserMapper userMapper;
    private final DingTalkNotifier dingTalkNotifier;

    public OutboundOrderServiceImpl(OutboundOrderMapper outboundOrderMapper,
                                    OutStorageItemMapper outStorageItemMapper,
                                    OutRecordMapper outRecordMapper,
                                    MaterialService materialService,
                                    DingTalkApprovalUtil dingTalkApprovalUtil,
                                    UserMapper userMapper,
                                    DingTalkNotifier dingTalkNotifier) {
        this.outboundOrderMapper = outboundOrderMapper;
        this.outStorageItemMapper = outStorageItemMapper;
        this.outRecordMapper = outRecordMapper;
        this.materialService = materialService;
        this.dingTalkApprovalUtil = dingTalkApprovalUtil;
        this.userMapper = userMapper;
        this.dingTalkNotifier = dingTalkNotifier;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveDraft(OutboundOrderDTO dto) {
        createDraft(dto);
    }

    private OutboundOrder createDraft(OutboundOrderDTO dto) {
        String code = generateOutboundCode();
        OutboundOrder order = new OutboundOrder();
        order.setOutboundCode(code);
        order.setOutType(dto.getOutType() == null ? 1 : dto.getOutType());
        order.setApplyUser(dto.getApplyUser());
        order.setRemark(dto.getRemark());
        order.setOrderStatus(0);
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        outboundOrderMapper.insert(order);

        List<OutStorageItemDTO> itemDTOList = dto.getItemList();
        if (itemDTOList == null || itemDTOList.isEmpty()) {
            throw new RuntimeException("出库明细不能为空");
        }
        List<OutStorageItem> itemList = new ArrayList<>();
        for (OutStorageItemDTO itemDTO : itemDTOList) {
            if (itemDTO.getOutNum() == null || itemDTO.getOutNum().compareTo(BigDecimal.ZERO) <= 0) {
                throw new RuntimeException("出库数量必须大于0");
            }
            if (itemDTO.getMaterialId() == null) {
                throw new RuntimeException("物料不能为空，请从物料库选择");
            }
            Material mat = materialService.getById(itemDTO.getMaterialId());
            if (mat == null) throw new RuntimeException("物料不存在: " + itemDTO.getMaterialId());
            BigDecimal available = mat.getStock().subtract(mat.getLockStock() != null ? mat.getLockStock() : BigDecimal.ZERO);
            if (available.compareTo(itemDTO.getOutNum()) < 0) {
                throw new RuntimeException("物料【" + mat.getMaterialName() + "】库存不足！可用:" + available + " 需要:" + itemDTO.getOutNum());
            }
            boolean locked = materialService.lockMaterialStock(itemDTO.getMaterialId(), itemDTO.getOutNum());
            if (!locked) {
                throw new RuntimeException("物料【" + mat.getMaterialName() + "】可用库存不足，无法占用");
            }
            OutStorageItem item = new OutStorageItem();
            item.setOutboundId(order.getId());
            item.setMaterialId(itemDTO.getMaterialId());
            item.setMaterialCode(itemDTO.getMaterialCode() != null ? itemDTO.getMaterialCode() : mat.getMaterialCode());
            item.setMaterialName(mat.getMaterialName());
            item.setBatchNo(itemDTO.getBatchNo());
            item.setOutNum(itemDTO.getOutNum());
            item.setCreateTime(LocalDateTime.now());
            itemList.add(item);
        }
        for (OutStorageItem item : itemList) {
            outStorageItemMapper.insert(item);
        }
        return order;
    }

    /** 生成出库单号：J + yyyyMMdd + 当日序号（如 J20260615-001）。 */
    private String generateOutboundCode() {
        String date = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE);
        LambdaQueryWrapper<OutboundOrder> dayQw = new LambdaQueryWrapper<OutboundOrder>()
                .likeRight(OutboundOrder::getOutboundCode, "J" + date)
                .orderByDesc(OutboundOrder::getOutboundCode)
                .last("limit 1");
        OutboundOrder last = outboundOrderMapper.selectOne(dayQw);
        int seq = 1;
        if (last != null && last.getOutboundCode() != null && last.getOutboundCode().contains("-")) {
            try {
                seq = Integer.parseInt(last.getOutboundCode().substring(last.getOutboundCode().lastIndexOf('-') + 1)) + 1;
            } catch (NumberFormatException ignore) {
                seq = 1;
            }
        }
        return "J" + date + "-" + String.format("%03d", seq);
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
        if (itemDTOList == null || itemDTOList.isEmpty()) {
            throw new RuntimeException("出库明细不能为空");
        }
        List<OutStorageItem> itemList = new ArrayList<>();
        for (OutStorageItemDTO itemDTO : itemDTOList) {
            if (itemDTO.getOutNum() == null || itemDTO.getOutNum().compareTo(BigDecimal.ZERO) <= 0) {
                throw new RuntimeException("出库数量必须大于0");
            }
            if (itemDTO.getMaterialId() == null) {
                throw new RuntimeException("物料不能为空，请从物料库选择");
            }
            Material mat = materialService.getById(itemDTO.getMaterialId());
            if (mat == null) throw new RuntimeException("物料不存在: " + itemDTO.getMaterialId());
            boolean locked = materialService.lockMaterialStock(itemDTO.getMaterialId(), itemDTO.getOutNum());
            if (!locked) {
                throw new RuntimeException("物料【" + mat.getMaterialName() + "】可用库存不足，无法占用");
            }
            OutStorageItem item = new OutStorageItem();
            item.setOutboundId(id);
            item.setMaterialId(itemDTO.getMaterialId());
            item.setMaterialCode(itemDTO.getMaterialCode() != null ? itemDTO.getMaterialCode() : mat.getMaterialCode());
            item.setMaterialName(mat.getMaterialName());
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
    public Page<OutboundOrder> getOrderPage(Integer pageNum, Integer pageSize, String outboundCode, Integer outType, Integer orderStatus, String keyword,
                                            String operator, String dataScope) {
        Page<OutboundOrder> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<OutboundOrder> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(OutboundOrder::getOutboundCode, keyword).or().like(OutboundOrder::getApplyUser, keyword));
        }
        if (StringUtils.hasText(outboundCode)) wrapper.like(OutboundOrder::getOutboundCode, outboundCode);
        if (outType != null) wrapper.eq(OutboundOrder::getOutType, outType);
        if (orderStatus != null) wrapper.eq(OutboundOrder::getOrderStatus, orderStatus);
        // 数据范围：仅本人/本部门/全部
        if ("self".equals(dataScope)) {
            wrapper.eq(OutboundOrder::getApplyUser, operator);
        }
        wrapper.orderByDesc(OutboundOrder::getCreateTime);
        return outboundOrderMapper.selectPage(page, wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String saveOrder(OutboundOrderDTO dto) {
        OutboundOrder order;
        if (dto.getId() == null) {
            // 前端直接提交（未先保存草稿）时，自动创建草稿并占用库存
            order = createDraft(dto);
        } else {
            order = outboundOrderMapper.selectById(dto.getId());
            if (order == null || !order.getOrderStatus().equals(0)) {
                throw new RuntimeException("单据状态异常，无法提交审批");
            }
        }

        List<OutStorageItem> itemList = outStorageItemMapper.selectByOutboundId(order.getId());
        if (itemList.isEmpty()) throw new RuntimeException("出库明细不能为空");
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
        if (itemList.isEmpty()) throw new RuntimeException("出库明细不能为空");

        for (OutStorageItem item : itemList) {
            if (item.getOutNum() == null || item.getOutNum().compareTo(BigDecimal.ZERO) <= 0) {
                throw new RuntimeException("出库数量必须大于0");
            }
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
            // 出库后库存低于物料阈值（minStock，默认5件）时通知库管员（需求 2.3.3）
            Material afterMat = materialService.getById(item.getMaterialId());
            if (afterMat != null) {
                java.math.BigDecimal threshold = afterMat.getMinStock() != null ? afterMat.getMinStock() : new BigDecimal("5");
                if (afterMat.getStock().compareTo(threshold) <= 0) {
                    dingTalkNotifier.sendText("库存不足预警",
                            "物料【" + afterMat.getMaterialName() + "】(编码:" + (afterMat.getMaterialCode() == null ? "-" : afterMat.getMaterialCode())
                                    + ") 出库后剩余 " + afterMat.getStock() + "，已达到阈值 " + threshold + "，请库管员及时补货。");
                }
            }
        }
        String dept = null;
        if (order.getApplyUser() != null) {
            User applier = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, order.getApplyUser()).last("limit 1"));
            if (applier != null) dept = applier.getDept();
        }
        for (OutStorageItem item : itemList) {
            OutRecord record = new OutRecord();
            record.setOutboundCode(order.getOutboundCode());
            record.setMaterialId(item.getMaterialId());
            record.setMaterialCode(item.getMaterialCode());
            record.setMaterialName(item.getMaterialName());
            record.setBatchNo(item.getBatchNo());
            record.setDept(dept);
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
            boolean unlocked = materialService.unLockMaterialStock(item.getMaterialId(), item.getOutNum());
            if (!unlocked) {
                throw new RuntimeException("库存解锁失败，请重试");
            }
        }
        order.setOrderStatus(2);
        order.setUpdateTime(LocalDateTime.now());
        outboundOrderMapper.updateById(order);
    }
}
