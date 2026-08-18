package com.koolearn.bms.enums;

import lombok.Getter;

@Getter
public enum OutTypeEnum {
    PRODUCE(1, "生产领料"),
    SALE(2, "销售出库"),
    RETURN(3, "退货出库");

    private final Integer code;
    private final String desc;

    OutTypeEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
