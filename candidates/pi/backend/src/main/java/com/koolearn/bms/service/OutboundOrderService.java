package com.koolearn.bms.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.koolearn.bms.dto.OutboundOrderDTO;
import com.koolearn.bms.entity.OutboundOrder;

public interface OutboundOrderService extends IService<OutboundOrder> {
    Long saveDraft(OutboundOrderDTO dto);
    void editDraft(Long id, OutboundOrderDTO dto);
    OutboundOrderDTO getDetailById(Long id);
    Page<OutboundOrder> getOrderPage(Integer pageNum, Integer pageSize, String outboundCode, Integer outType, Integer orderStatus, String keyword, String applyUser);
    String saveOrder(OutboundOrderDTO dto);
    void confirmOut(Long outboundId, String operUser);
    void rejectOut(Long id);
    OutboundOrder getByDingInstanceId(String instanceId);
    /** 钉钉回调同意：仅当单据仍处于待审批(0)时保持状态不变（防旧回调回写已处理单据） */
    boolean approveFromCallback(Long id);
}
