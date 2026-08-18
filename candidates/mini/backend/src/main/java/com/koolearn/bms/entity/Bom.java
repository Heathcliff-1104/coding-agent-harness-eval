package com.koolearn.bms.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("tb_bom")
public class Bom {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String bomCode;
    private String bomName;
    private String version;
    private Integer status;
    private String creator;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
