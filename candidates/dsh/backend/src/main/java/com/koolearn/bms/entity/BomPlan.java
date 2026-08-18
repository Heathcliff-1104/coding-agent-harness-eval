package com.koolearn.bms.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("bom_plan")
public class BomPlan {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String planNo;
    private String bomVersion;
    private Long materialId;
    private String materialCode;
    private String materialName;
    private String packageType;
    private String valueData;
    private String specModel;
    private String batchNo;
    private BigDecimal needNum;
    private BigDecimal currentStock;
    private BigDecimal shortage;
    private String stockStatus;
    private String remark;
    private String createBy;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
