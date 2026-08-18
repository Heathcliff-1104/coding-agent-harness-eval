package com.koolearn.bms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
    public IPage<InboundOrder> getOrderPage(Long pageNum, Long pageSize, String billNo, String supplier, Integer status, String keyword, Integer inType,
                                            String startDate, String endDate, String materialName) {
        Page<InboundOrder> page = new Page<>(pageNum, pageSize);
        return baseMapper.selectOrderPage(page, billNo, supplier, status, keyword, inType, startDate, endDate, materialName);
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

        List<InStorageItem> itemList = inStorageItemMapper.selectByInboundId(inboundId);
        if (CollectionUtils.isEmpty(itemList)) throw new RuntimeException("入库明细不能为空");

        for (InStorageItem item : itemList) {
            if (item.getNum() == null || item.getNum().compareTo(BigDecimal.ZERO) <= 0) {
                throw new RuntimeException("入库数量必须大于0");
            }
            // 1) 确认物料（新物料自动建档并生成物料编码）
            Material mat = resolveOrCreateMaterial(item);
            item.setMaterialId(mat.getId());

            // 2) 增加库存（失败即抛异常，事务回滚）
            boolean added = materialService.addStock(mat.getId(), item.getNum());
            if (!added) {
                throw new RuntimeException("物料【" + mat.getMaterialName() + "】库存更新失败，请重试");
            }
            // 重新读取，避免用内存中的旧值覆盖新库存
            mat = materialService.getById(mat.getId());
            if (!StringUtils.hasText(mat.getMaterialCode())) {
                String autoCode = generateMaterialCode(mat.getId());
                mat.setMaterialCode(autoCode);
                materialService.updateById(mat);
            }

            // 3) 写入入库记录（不可删除的流水）
            InRecord record = new InRecord();
            record.setBillNo(order.getBillNo());
            record.setMaterialId(mat.getId());
            record.setMaterialCode(mat.getMaterialCode());
            record.setMaterialName(mat.getMaterialName());
            record.setBatchNo(item.getBatchNo());
            record.setLocationNo(item.getLocationNo());
            record.setSupplier(order.getSupplier());
            record.setInNum(item.getNum());
            record.setInUser(operUser);
            record.setInTime(LocalDateTime.now());
            recordMapper.insert(record);
        }

        order.setOrderStatus(1);
        order.setInDate(order.getInDate() == null ? LocalDateTime.now() : order.getInDate());
        updateById(order);
    }

    /** 按物料ID/编码/名称匹配已有物料；没有则按明细信息新建物料（自动生成编码）。 */
    private Material resolveOrCreateMaterial(InStorageItem item) {
        if (item.getMaterialId() != null) {
            Material m = materialService.getById(item.getMaterialId());
            if (m != null) return m;
        }
        if (StringUtils.hasText(item.getMaterialCode())) {
            Material byCode = materialService.getOne(new LambdaQueryWrapper<Material>()
                    .eq(Material::getMaterialCode, item.getMaterialCode()).last("limit 1"));
            if (byCode != null) return byCode;
        }
        if (StringUtils.hasText(item.getMaterialName())) {
            Material byName = materialService.getOne(new LambdaQueryWrapper<Material>()
                    .eq(Material::getMaterialName, item.getMaterialName()).last("limit 1"));
            if (byName != null) return byName;
        }
        if (!StringUtils.hasText(item.getMaterialName())) {
            throw new RuntimeException("入库明细缺少物料名称，无法创建物料");
        }
        Material mat = new Material();
        mat.setMaterialName(item.getMaterialName());
        mat.setPackageType(item.getPackageType());
        mat.setValueData(item.getValueData());
        mat.setSpecModel(item.getSpecModel());
        mat.setManufacturer(item.getManufacturer());
        mat.setLocationNo(item.getLocationNo());
        mat.setRemark(item.getRemark());
        mat.setStock(BigDecimal.ZERO);
        mat.setLockStock(BigDecimal.ZERO);
        mat.setMinStock(BigDecimal.ZERO);
        mat.setMaxStock(null);
        materialService.save(mat);
        return mat;
    }

    /** 生成物料编码：MTR-yyyyMMdd-4位序号（基于新物料主键，无碰撞）。 */
    private String generateMaterialCode(Long materialId) {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return "MTR-" + date + "-" + String.format("%04d", materialId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void editDraft(InboundOrderDTO dto, Long id) {
        InboundOrder order = getById(id);
        if (order == null) throw new RuntimeException("单据不存在");
        order.setBillNo(dto.getBillNo());
        order.setInType(dto.getInType() == null ? order.getInType() : dto.getInType());
        order.setReturnReason(dto.getReturnReason());
        order.setSupplier(dto.getSupplier());
        order.setApplyUser(dto.getUserName());
        order.setRemark(dto.getRemark());
        if (dto.getInDate() != null && !dto.getInDate().trim().isEmpty()) {
            try {
                order.setInDate(LocalDateTime.parse(dto.getInDate() + " 00:00:00",
                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            } catch (Exception e) {
                throw new RuntimeException("日期格式错误，请使用 yyyy-MM-dd");
            }
        }
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
        dto.setId(order.getId());
        dto.setBillNo(order.getBillNo());
        dto.setInType(order.getInType());
        dto.setReturnReason(order.getReturnReason());
        dto.setSupplier(order.getSupplier());
        dto.setUserName(order.getApplyUser());
        dto.setInDate(order.getInDate() == null ? null :
                order.getInDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        dto.setRemark(order.getRemark());
        dto.setItemList(itemList);
        return dto;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String saveOrder(InboundOrderDTO dto) {
        // 幂等：已存在草稿（按id或单号）则复用，避免重复单据
        InboundOrder order = null;
        if (dto.getId() != null) {
            order = getById(dto.getId());
        }
        if (order == null && StringUtils.hasText(dto.getBillNo())) {
            order = getOne(new LambdaQueryWrapper<InboundOrder>()
                    .eq(InboundOrder::getBillNo, dto.getBillNo())
                    .orderByDesc(InboundOrder::getCreateTime)
                    .last("limit 1"));
        }
        if (order == null) {
            order = buildOrder(dto);
            order.setOrderStatus(0);
            baseMapper.insert(order);
        } else {
            order.setSupplier(dto.getSupplier());
            order.setApplyUser(dto.getUserName());
            order.setRemark(dto.getRemark());
            order.setInType(dto.getInType() == null ? order.getInType() : dto.getInType());
            baseMapper.updateById(order);
            inStorageItemMapper.deleteByInboundId(order.getId());
        }

        Long orderId = order.getId();
        List<InStorageItem> itemList = dto.getItemList();
        if (CollectionUtils.isEmpty(itemList)) {
            throw new RuntimeException("入库明细不能为空");
        }
        fillMaterialInfo(itemList);
        for (InStorageItem item : itemList) {
            if (item.getNum() == null || item.getNum().compareTo(BigDecimal.ZERO) <= 0) {
                throw new RuntimeException("入库数量必须大于0");
            }
            item.setInboundId(orderId);
            item.setCreateTime(LocalDateTime.now());
        }
        inStorageItemMapper.insertBatch(itemList);

        try {
            ArrayNode table = MAPPER.createArrayNode();
            for (InStorageItem item : itemList) {
                ArrayNode row = MAPPER.createArrayNode();
                row.add(item.getMaterialCode() != null ? item.getMaterialCode() : item.getMaterialName());
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
        order.setInType(dto.getInType() == null ? 1 : dto.getInType());
        order.setReturnReason(dto.getReturnReason());
        order.setSupplier(dto.getSupplier());
        order.setApplyUser(dto.getUserName());
        order.setRemark(dto.getRemark());
        if (dto.getInDate() != null && !dto.getInDate().trim().isEmpty()) {
            try {
                order.setInDate(LocalDateTime.parse(dto.getInDate() + " 00:00:00",
                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
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
            if (item.getMaterialId() != null) {
                Material mat = materialService.getById(item.getMaterialId());
                if (mat != null) {
                    item.setMaterialCode(mat.getMaterialCode());
                    item.setMaterialName(mat.getMaterialName());
                    item.setPackageType(mat.getPackageType());
                    item.setValueData(mat.getValueData());
                    item.setSpecModel(mat.getSpecModel());
                    item.setManufacturer(mat.getManufacturer());
                    if (!StringUtils.hasText(item.getLocationNo())) {
                        item.setLocationNo(mat.getLocationNo());
                    }
                }
            } else if (StringUtils.hasText(item.getMaterialCode())) {
                Material mat = materialService.getOne(new LambdaQueryWrapper<Material>()
                        .eq(Material::getMaterialCode, item.getMaterialCode()).last("limit 1"));
                if (mat != null) {
                    item.setMaterialId(mat.getId());
                    item.setMaterialName(mat.getMaterialName());
                }
            }
        }
    }
}
