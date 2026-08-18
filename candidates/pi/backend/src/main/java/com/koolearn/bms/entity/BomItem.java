package com.koolearn.bms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("bom_item")
public class BomItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long bomId;
    private Long materialId;
    private String materialCode;
    private String materialName;
    private String packageType;
    private String specModel;
    private String batchNo;
    private BigDecimal needNum;
    private String remark;
}
