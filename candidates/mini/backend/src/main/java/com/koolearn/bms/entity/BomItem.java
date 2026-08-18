package com.koolearn.bms.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
@TableName("tb_bom_item")
public class BomItem {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long bomId;
    private Long materialId;
    private String materialCode;
    private String materialName;
    private String packageType;
    private String valueData;
    private String specModel;
    private String batchNo;
    private BigDecimal needNum;
    private String remark;
}
