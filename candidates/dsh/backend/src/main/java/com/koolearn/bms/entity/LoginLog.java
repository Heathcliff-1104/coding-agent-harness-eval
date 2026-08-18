package com.koolearn.bms.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_login_log")
public class LoginLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private String loginIp;
    private String deviceInfo;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime loginTime;
    private Integer loginResult;
}
