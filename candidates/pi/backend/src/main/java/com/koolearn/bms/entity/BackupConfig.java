package com.koolearn.bms.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("backup_config")
public class BackupConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** full=全量 incremental=增量 */
    private String backupType;

    private String cronExpr;
    private Integer retentionDays;
    private Integer enabled;
    private LocalDateTime lastRun;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
