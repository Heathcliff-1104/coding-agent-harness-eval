package com.koolearn.bms.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("cis_sync_log")
public class CisSyncLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** full/incremental */
    private String syncType;
    /** success/failed */
    private String syncStatus;
    private Integer materialCount;
    private String message;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
