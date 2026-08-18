package com.koolearn.bms.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.koolearn.bms.dto.InboundOrderDTO;
import com.koolearn.bms.entity.InboundOrder;
import com.baomidou.mybatisplus.core.metadata.IPage;


public interface InboundOrderService extends IService<InboundOrder> {
    //原有旧方法保留不动
    boolean createOrder(InboundOrder order);
    InboundOrder getByDingInstanceId(String instanceId);
    void saveDraft(InboundOrderDTO dto);
    void confirmIn(Long inboundId, String operUser);
    // 修改草稿
    void editDraft(InboundOrderDTO dto, Long id);

    //新增：前端整单提交（表头+明细、自动发钉钉审批）
    String saveOrder(InboundOrderDTO dto);
    //接口InboundOrderService
    InboundOrderDTO getDetailById(Long id);
    // 新增：分页查询入库单（applyUser 非空时按申请人过滤，用于数据范围）
    IPage<InboundOrder> getOrderPage(Long pageNum, Long pageSize, String billNo, String supplier, Integer status, String keyword, String applyUser);
    /** 钉钉回调同意：仅当单据仍处于待审批(0)时保持状态不变（防旧回调回写已处理单据） */
    boolean approveFromCallback(Long id);
    /** 钉钉回调驳回：待审批(0) -> 已拒绝(2) */
    boolean refuseFromCallback(Long id);
}
