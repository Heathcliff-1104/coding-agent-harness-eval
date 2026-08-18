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
@TableName("in_record")
public class InRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    // 修复：数据库 bill_no 对应 billNo，删除原错误 inboundCode
    private String billNo;
    private Long materialId;
    private String materialCode;
    private String materialName;
    private String batchNo;
    private String locationNo;
    private String supplier;
    private BigDecimal inNum;
    private String inUser;
    private LocalDateTime inTime;
    // 补充数据库 create_time
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}