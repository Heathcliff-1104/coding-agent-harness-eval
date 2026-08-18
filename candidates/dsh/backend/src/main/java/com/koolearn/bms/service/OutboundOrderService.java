package com.koolearn.bms.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.koolearn.bms.dto.OutboundOrderDTO;
import com.koolearn.bms.entity.OutboundOrder;

public interface OutboundOrderService extends IService<OutboundOrder> {
    void saveDraft(OutboundOrderDTO dto);
    void editDraft(Long id, OutboundOrderDTO dto);
    OutboundOrderDTO getDetailById(Long id);
    Page<OutboundOrder> getOrderPage(Integer pageNum, Integer pageSize, String outboundCode, Integer outType, Integer orderStatus, String keyword,
                                     String operator, String dataScope);
    String saveOrder(OutboundOrderDTO dto);
    void confirmOut(Long outboundId, String operUser);
    void rejectOut(Long id);
    OutboundOrder getByDingInstanceId(String instanceId);
    boolean updateStatus(Long id, Integer status);
}
