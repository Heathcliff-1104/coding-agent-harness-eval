package com.koolearn.bms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("cis_sync_log")
public class CisSyncLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** full=全量 incremental=增量 */
    private String syncType;

    /** SUCCESS / FAILED / SKIPPED */
    private String status;

    private Integer rows;

    private String message;

    private LocalDateTime syncTime;
}
