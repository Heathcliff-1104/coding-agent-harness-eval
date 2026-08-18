package com.koolearn.bms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_permission")
public class Permission {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 权限编码：菜单为路由路径（如 /inbound/purchase），按钮为按钮码（如 inbound:confirm） */
    private String permCode;

    private String permName;

    /** menu=菜单 button=按钮 */
    private String permType;

    private String parentCode;

    /** 前端路由路径（菜单） */
    private String path;

    private Integer sortNo;

    private LocalDateTime createTime;
}
