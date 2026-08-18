package com.koolearn.bms.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("sys_restock_request")
public class SysRestockRequest {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long materialId;
    private String materialCode;
    private String materialName;
    private String supplierContact;
    private BigDecimal purchaseQty;
    private String requester;
    private Integer status;   // 0 待处理 1 已采购
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
