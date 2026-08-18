package com.koolearn.bms.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("purchase_request")
public class PurchaseRequest {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long materialId;
    private String materialCode;
    private String materialName;
    private String manufacturer;
    private BigDecimal quantity;
    private String remark;
    private String status;
    private String createBy;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
