package com.koolearn.bms.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("sys_stock_alert")
public class StockAlert {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long materialId;
    private String materialCode;
    private String materialName;
    private Integer alertType;
    private BigDecimal currentStock;
    private BigDecimal thresholdStock;
    private Integer handled;
    private String handler;
    private String handleMethod;
    private LocalDateTime handleTime;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
