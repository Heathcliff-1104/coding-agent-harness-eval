package com.koolearn.bms.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
@TableName("tb_pick_plan_item")
public class PickPlanItem {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long planId;
    private String materialCode;
    private String materialName;
    private String packageType;
    private String valueData;
    private String specModel;
    private BigDecimal stock;
    private BigDecimal needNum;
    private BigDecimal supplementNum;
    private String remark;
}
