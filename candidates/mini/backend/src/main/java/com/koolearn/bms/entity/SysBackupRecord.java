package com.koolearn.bms.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_backup_record")
public class SysBackupRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String backupType;   // full / incremental
    private String filePath;
    private Long fileSize;
    private String status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
