package com.koolearn.bms.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("sys_replenishment")
public class Replenishment {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long materialId;
    private String materialCode;
    private String materialName;
    private BigDecimal shortage;
    private String supplierContact;
    private BigDecimal purchaseNum;
    private String applicant;

    /** 0=待处理 1=已处理 */
    private Integer status;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
