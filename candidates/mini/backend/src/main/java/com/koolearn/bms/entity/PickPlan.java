package com.koolearn.bms.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("tb_pick_plan")
public class PickPlan {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String planNo;
    private Long bomId;
    private String bomVersion;
    private String creator;
    private String remark;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
