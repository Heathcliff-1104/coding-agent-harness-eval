package com.koolearn.bms.enums;

import lombok.Getter;

@Getter
public enum OrderStatusEnum {
    WAIT_AUDIT(0, "待审批"),
    COMPLETE(1, "已完成/已出入库"),
    REJECT(2, "已驳回");

    private final Integer code;
    private final String desc;

    OrderStatusEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}