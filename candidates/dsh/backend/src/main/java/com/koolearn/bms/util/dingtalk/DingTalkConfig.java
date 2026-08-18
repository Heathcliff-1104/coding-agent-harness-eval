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

    /** 演示模式开关：app.key 为 demo-key 或显式配置 dingtalk.mock.enabled=true 时启用 */
    @Value("${dingtalk.mock.enabled:false}")
    private boolean mockEnabled;

    /** 机器人Webhook（发送通知用），为空时通知仅记录日志 */
    @Value("${dingtalk.notify.webhook:}")
    private String notifyWebhook;

    /** CIS系统同步地址（示例 CIS 元件库），空/演示模式下不调用 */
    @Value("${cis.endpoint:}")
    private String cisEndpoint;

    public boolean isMockEnabled() {
        return mockEnabled || "demo-key".equals(appKey);
    }
}
