package com.koolearn.bms.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("tb_inbound_order")
public class InboundOrder {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String billNo;
    private String supplier;
    private String applyUser;
    // 补充数据库字段 apply_time
    private LocalDateTime applyTime;
    private LocalDateTime inDate;
    /** 入库类型: PURCHASE=采购入库 RETURN=退库入库 */
    private String inType;
    /** 退库原因 */
    private String returnReason;
    private String remark;
    private String dingInstanceId;
    // 0待审批 1已入库 2已拒绝
    private Integer orderStatus;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private List<InStorageItem> itemList;
}