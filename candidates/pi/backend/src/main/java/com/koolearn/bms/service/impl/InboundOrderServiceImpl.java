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
import com.koolearn.bms.service.CisSyncService;
import com.koolearn.bms.service.DingTalkNotifier;
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
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
public class InboundOrderServiceImpl extends ServiceImpl<InboundOrderMapper, InboundOrder> implements InboundOrderService {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final ExecutorService CIS_EXECUTOR = Executors.newSingleThreadExecutor();

    private final InStorageItemMapper inStorageItemMapper;
    private final DingTalkApprovalUtil dingTalkApprovalUtil;
    private final MaterialService materialService;
    private final InRecordMapper recordMapper;
    private final CisSyncService cisSyncService;
    private final DingTalkNotifier dingTalkNotifier;

    public InboundOrderServiceImpl(InStorageItemMapper inStorageItemMapper,
                                   DingTalkApprovalUtil dingTalkApprovalUtil,
                                   MaterialService materialService,
                                   InRecordMapper recordMapper,
                                   CisSyncService cisSyncService,
                                   DingTalkNotifier dingTalkNotifier) {
        this.inStorageItemMapper = inStorageItemMapper;
        this.dingTalkApprovalUtil = dingTalkApprovalUtil;
        this.materialService = materialService;
        this.recordMapper = recordMapper;
        this.cisSyncService = cisSyncService;
        this.dingTalkNotifier = dingTalkNotifier;
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
    public boolean approveFromCallback(Long id) {
        return baseMapper.approveFromCallback(id) > 0;
    }

    @Override
    public boolean refuseFromCallback(Long id) {
        return baseMapper.refuseFromCallback(id) > 0;
    }

    @Override
    public IPage<InboundOrder> getOrderPage(Long pageNum, Long pageSize, String billNo, String supplier, Integer status, String keyword, String applyUser) {
        Page<InboundOrder> page = new Page<>(pageNum, pageSize);
        return baseMapper.selectOrderPage(page, billNo, supplier, status, keyword, applyUser);
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
        // 原子状态守卫（第一步）：0 -> 1，防止 TOCTOU（两请求同时确认）。
        // 若 0 行受影响说明状态已变化（已入库/已拒绝），抛异常由事务回滚。
        if (baseMapper.atomicConfirm(inboundId) == 0) {
            throw new RuntimeException("单据状态异常或已处理");
        }

        List<InStorageItem> itemList = inStorageItemMapper.selectByInboundId(inboundId);
        if (CollectionUtils.isEmpty(itemList)) throw new RuntimeException("入库明细不能为空");

        for (InStorageItem item : itemList) {
            Material mat = materialService.getById(item.getMaterialId());
            if (mat == null) throw new RuntimeException("物料不存在: " + item.getMaterialId());
            boolean needUpdate = false;
            if (mat.getMaterialCode() == null || mat.getMaterialCode().isEmpty()) {
                // 自动生成物料编码
                mat.setMaterialCode("MTR-" + java.time.LocalDate.now()
                        .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"))
                        + "-" + String.format("%04d", item.getMaterialId()));
                needUpdate = true;
            }
            if (item.getLocationNo() != null && !item.getLocationNo().isEmpty()) {
                mat.setLocationNo(item.getLocationNo());
                needUpdate = true;
            }
            // 先更新物料信息，再增加库存（避免乐观锁版本冲突）
            if (needUpdate) {
                materialService.updateById(mat);
            }
            materialService.addStock(item.getMaterialId(), item.getNum());

            InRecord record = new InRecord();
            record.setBillNo(order.getBillNo());
            record.setMaterialId(item.getMaterialId());
            record.setMaterialCode(mat.getMaterialCode());
            record.setMaterialName(mat.getMaterialName());
            record.setBatchNo(item.getBatchNo());
            record.setInNum(item.getNum());
            record.setInUser(operUser);
            record.setInTime(LocalDateTime.now());
            record.setLocationNo(item.getLocationNo());
            recordMapper.insert(record);
        }
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
    @Transactional(rollbackFor = Exception.class)
    public void editDraft(InboundOrderDTO dto, Long id) {
        InboundOrder order = getById(id);
        if (order == null) throw new RuntimeException("单据不存在");
        // 仅待审批(0)状态允许修改草稿
        if (order.getOrderStatus() == null || order.getOrderStatus() != 0) {
            throw new RuntimeException("仅草稿状态可修改");
        }
        // 对比明细变化，若增删则通知申请人
        List<InStorageItem> oldItems = inStorageItemMapper.selectByInboundId(id);
        int oldCount = oldItems == null ? 0 : oldItems.size();
        int newCount = dto.getItemList() == null ? 0 : dto.getItemList().size();
        order.setBillNo(dto.getBillNo());
        order.setSupplier(dto.getSupplier());
        order.setApplyUser(dto.getUserName());
        order.setInType(dto.getInType());
        order.setReturnReason(dto.getReturnReason());
        order.setRemark(dto.getRemark());
        order.setUpdateTime(LocalDateTime.now());
        updateById(order);

        inStorageItemMapper.deleteByInboundId(id);
        saveItems(id, dto.getItemList());

        if (oldCount != newCount || (dto.getRemark() != null && !dto.getRemark().isEmpty())) {
            try {
                dingTalkNotifier.send("入库单物料变更", "入库单【" + dto.getBillNo() + "】物料明细有增删，原因: "
                        + (dto.getRemark() == null ? "" : dto.getRemark()), Arrays.asList(order.getApplyUser()));
            } catch (Exception e) {
                log.warn("入库变更通知失败: {}", e.getMessage());
            }
        }
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
        dto.setInType(order.getInType());
        dto.setReturnReason(order.getReturnReason());
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
        order.setInType(dto.getInType());
        order.setReturnReason(dto.getReturnReason());
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
