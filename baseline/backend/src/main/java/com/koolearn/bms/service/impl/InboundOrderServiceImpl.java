package com.koolearn.bms.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.koolearn.bms.dto.InboundOrderDTO;
import com.koolearn.bms.entity.InboundOrder;
import com.koolearn.bms.entity.InRecord;
import com.koolearn.bms.entity.InStorageItem;
import com.koolearn.bms.entity.Material;
import com.koolearn.bms.mapper.InRecordMapper;
import com.koolearn.bms.mapper.InStorageItemMapper;
import com.koolearn.bms.mapper.InboundOrderMapper;
import com.koolearn.bms.service.InboundOrderService;
import com.koolearn.bms.service.MaterialService;
import com.koolearn.bms.util.dingtalk.DingTalkApprovalUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class InboundOrderServiceImpl extends ServiceImpl<InboundOrderMapper, InboundOrder> implements InboundOrderService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final InStorageItemMapper inStorageItemMapper;
    private final DingTalkApprovalUtil dingTalkApprovalUtil;
    private final MaterialService materialService;
    private final InRecordMapper recordMapper;

    public InboundOrderServiceImpl(InStorageItemMapper inStorageItemMapper,
                                   DingTalkApprovalUtil dingTalkApprovalUtil,
                                   MaterialService materialService,
                                   InRecordMapper recordMapper) {
        this.inStorageItemMapper = inStorageItemMapper;
        this.dingTalkApprovalUtil = dingTalkApprovalUtil;
        this.materialService = materialService;
        this.recordMapper = recordMapper;
    }

    @Override
    public boolean createOrder(InboundOrder order) {
        return baseMapper.insert(order) > 0;
    }

    @Override
    public InboundOrder getByDingInstanceId(String instanceId) {
        return baseMapper.selectByDingInstanceId(instanceId);
    }

    @Override
    public boolean updateStatus(Long id, Integer status) {
        return baseMapper.updateOrderStatus(id, status) > 0;
    }

    @Override
    public IPage<InboundOrder> getOrderPage(Long pageNum, Long pageSize, String billNo, String supplier, Integer status, String keyword) {
        Page<InboundOrder> page = new Page<>(pageNum, pageSize);
        return baseMapper.selectOrderPage(page, billNo, supplier, status, keyword);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveDraft(InboundOrderDTO dto) {
        InboundOrder order = buildOrder(dto);
        order.setOrderStatus(0);
        save(order);
        saveItems(order.getId(), dto.getItemList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmIn(Long inboundId, String operUser) {
        InboundOrder order = getById(inboundId);
        if (order == null) throw new RuntimeException("入库单不存在");
        if (order.getOrderStatus() == 1) throw new RuntimeException("该单据已入库，请勿重复操作");

        order.setOrderStatus(1);
        updateById(order);

        List<InStorageItem> itemList = inStorageItemMapper.selectByInboundId(inboundId);
        if (CollectionUtils.isEmpty(itemList)) throw new RuntimeException("入库明细不能为空");

        for (InStorageItem item : itemList) {
            materialService.addStock(item.getMaterialId(), item.getNum());

            InRecord record = new InRecord();
            record.setBillNo(order.getBillNo());
            record.setMaterialId(item.getMaterialId());
            record.setBatchNo(item.getBatchNo());
            record.setInNum(item.getNum());
            record.setInUser(operUser);
            record.setInTime(LocalDateTime.now());
            recordMapper.insert(record);

            Material mat = materialService.getById(item.getMaterialId());
            if (mat != null && (mat.getMaterialCode() == null || mat.getMaterialCode().isEmpty())) {
                String autoCode = "MTR-" + java.time.LocalDate.now()
                        .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"))
                        + "-" + String.format("%04d", item.getMaterialId());
                mat.setMaterialCode(autoCode);
                materialService.updateById(mat);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void editDraft(InboundOrderDTO dto, Long id) {
        InboundOrder order = getById(id);
        if (order == null) throw new RuntimeException("单据不存在");
        order.setBillNo(dto.getBillNo());
        order.setSupplier(dto.getSupplier());
        order.setApplyUser(dto.getUserName());
        order.setRemark(dto.getRemark());
        order.setUpdateTime(LocalDateTime.now());
        updateById(order);

        inStorageItemMapper.deleteByInboundId(id);
        saveItems(id, dto.getItemList());
    }

    @Override
    public InboundOrderDTO getDetailById(Long id) {
        InboundOrder order = getById(id);
        if (order == null) return null;
        List<InStorageItem> itemList = inStorageItemMapper.selectByInboundId(id);
        InboundOrderDTO dto = new InboundOrderDTO();
        dto.setBillNo(order.getBillNo());
        dto.setSupplier(order.getSupplier());
        dto.setUserName(order.getApplyUser());
        dto.setInDate(order.getInDate() == null ? null :
                order.getInDate().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        dto.setRemark(order.getRemark());
        dto.setItemList(itemList);
        return dto;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String saveOrder(InboundOrderDTO dto) {
        InboundOrder order = buildOrder(dto);
        order.setOrderStatus(0);
        baseMapper.insert(order);

        Long orderId = order.getId();
        List<InStorageItem> itemList = dto.getItemList();
        if (!CollectionUtils.isEmpty(itemList)) {
            fillMaterialInfo(itemList);
            for (InStorageItem item : itemList) {
                item.setInboundId(orderId);
                item.setCreateTime(LocalDateTime.now());
            }
            inStorageItemMapper.insertBatch(itemList);
        }

        try {
            ArrayNode table = MAPPER.createArrayNode();
            for (InStorageItem item : itemList) {
                ArrayNode row = MAPPER.createArrayNode();
                row.add(item.getMaterialCode());
                row.add(item.getNum().toString());
                table.add(row);
            }
            String instanceId = dingTalkApprovalUtil.createInboundApproval(dto.getBillNo(), table.toString());
            order.setDingInstanceId(instanceId);
            updateById(order);
            return instanceId;
        } catch (Exception e) {
            log.warn("钉钉审批发起失败: {}", e.getMessage());
            return null;
        }
    }

    private InboundOrder buildOrder(InboundOrderDTO dto) {
        InboundOrder order = new InboundOrder();
        order.setBillNo(dto.getBillNo());
        order.setSupplier(dto.getSupplier());
        order.setApplyUser(dto.getUserName());
        order.setRemark(dto.getRemark());
        if (dto.getInDate() != null && !dto.getInDate().trim().isEmpty()) {
            try {
                order.setInDate(LocalDateTime.parse(dto.getInDate() + " 00:00:00",
                        java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            } catch (Exception e) {
                throw new RuntimeException("日期格式错误，请使用 yyyy-MM-dd");
            }
        }
        LocalDateTime now = LocalDateTime.now();
        order.setApplyTime(now);
        order.setCreateTime(now);
        order.setUpdateTime(now);
        return order;
    }

    private void saveItems(Long orderId, List<InStorageItem> itemList) {
        inStorageItemMapper.deleteByInboundId(orderId);
        if (!CollectionUtils.isEmpty(itemList)) {
            for (InStorageItem item : itemList) {
                item.setInboundId(orderId);
                item.setCreateTime(LocalDateTime.now());
            }
            fillMaterialInfo(itemList);
            inStorageItemMapper.insertBatch(itemList);
        }
    }

    private void fillMaterialInfo(List<InStorageItem> itemList) {
        for (InStorageItem item : itemList) {
            Material mat = materialService.getById(item.getMaterialId());
            if (mat != null) {
                item.setMaterialCode(mat.getMaterialCode());
            }
        }
    }
}
