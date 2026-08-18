package com.koolearn.bms.util.dingtalk;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Getter
@Component
public class DingTalkConfig {

    @Value("${dingtalk.app.key}")
    private String appKey;

    @Value("${dingtalk.app.secret}")
    private String appSecret;

    @Value("${dingtalk.corp.id}")
    private String corpId;

    @Value("${dingtalk.agent.id}")
    private Long agentId;

    @Value("${dingtalk.inbound.process-code}")
    private String inboundProcessCode;

    @Value("${dingtalk.outbound.process-code}")
    private String outboundProcessCode;

    @Value("${dingtalk.originator.user-id}")
    private String originatorUserId;

    @Value("${dingtalk.dept.id}")
    private String deptId;
}
