package com.koolearn.bms.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_cis_sync_log")
public class SysCisSyncLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String syncType;   // full / incremental
    private String syncMode;   // mock / http
    private Integer totalCount;
    private Integer successCount;
    private Integer failCount;
    private String status;     // SUCCESS / FAILED
    private String errorMsg;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
