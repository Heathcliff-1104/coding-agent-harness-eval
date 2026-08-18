package com.koolearn.bms.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("tb_bom_match_history")
public class BomMatchHistory {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long bomId;
    private String bomVersion;
    private String materialCode;
    private String materialName;
    private String stockStatus;   // sufficient / insufficient / out_of_stock / occupied
    private BigDecimal currentStock;
    private BigDecimal needNum;
    private BigDecimal shortage;
    private String outboundCode;
    private String remark;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime matchTime;
}
