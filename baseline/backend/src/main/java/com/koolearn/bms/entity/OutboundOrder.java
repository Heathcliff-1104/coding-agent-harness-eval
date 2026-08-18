package com.koolearn.bms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("tb_outbound_order")
public class OutboundOrder {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String outboundCode;
    private Integer outType;
    private String applyUser;
    private String operUser;
    private String remark;
    private Integer orderStatus;
    private String dingInstanceId;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}