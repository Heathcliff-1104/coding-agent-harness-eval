package com.koolearn.bms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Data
@TableName("tb_out_record")
public class OutRecord {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String outboundCode;
    private Long materialId;
    private String batchNo;
    private BigDecimal outNum;
    private String outUser;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime outTime;

    // 关联物料信息（非表字段，查询时通过 LEFT JOIN 填充）
    @TableField(exist = false)
    private String materialCode;
    @TableField(exist = false)
    private String materialName;
}