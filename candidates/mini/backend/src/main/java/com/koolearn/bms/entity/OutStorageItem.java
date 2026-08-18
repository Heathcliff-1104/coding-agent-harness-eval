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
@TableName("tb_out_storage_item")
public class OutStorageItem {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long outboundId;
    private Long materialId;
    private String materialCode;
    private String batchNo;
    private BigDecimal outNum;
    // 新增创建时间字段
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}