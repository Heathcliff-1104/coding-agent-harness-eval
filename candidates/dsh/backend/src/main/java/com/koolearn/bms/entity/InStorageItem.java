package com.koolearn.bms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("in_storage_item")
public class InStorageItem {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long inboundId;
    private Long materialId;
    private String materialCode;
    private String materialName;
    private String packageType;
    private String valueData;
    private String specModel;
    private String manufacturer;
    private BigDecimal num;
    private String batchNo;
    private String locationNo;
    private String remark;
    // 补充数据库创建时间
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}