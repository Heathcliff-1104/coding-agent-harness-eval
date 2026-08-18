package com.koolearn.bms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("tb_material")
public class Material {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String materialCode;    // 物料编码
    private String materialName;    // 物料名称
    private String packageType;     // 封装
    private String valueData;       // value值
    private String specModel;       // 规格型号
    private String warehouseCode;   // 仓库编码
    private String locationNo;      // 库位
    private String remark;          // 备注
    private BigDecimal stock;       // 当前总库存

    @Version
    private Integer version; // 乐观锁版本号

    private BigDecimal lockStock;    // 锁定占用库存(未审批单据占用)
    private BigDecimal minStock;     // 最低预警库存
    private BigDecimal maxStock;     // 最高超储库存
    private Integer stagnationDays;  // 呆滞判定天数
    private BigDecimal materialCost; // 物料单件成本

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}