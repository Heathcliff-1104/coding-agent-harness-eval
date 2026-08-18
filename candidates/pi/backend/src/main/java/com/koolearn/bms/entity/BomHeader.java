package com.koolearn.bms.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("bom_header")
public class BomHeader {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String bomNo;
    private String bomName;
    private Integer version;
    private Integer repeatFlag;
    private String createUser;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
