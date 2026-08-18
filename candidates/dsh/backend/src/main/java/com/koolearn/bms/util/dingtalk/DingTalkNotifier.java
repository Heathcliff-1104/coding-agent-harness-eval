package com.koolearn.bms.util.dingtalk;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 钉钉消息通知工具（机器人 Webhook）。
 * - 配置 dingtalk.notify.webhook 后调用真实钉钉机器人。
 * - 未配置或演示模式（mock）下仅记录日志，返回模拟结果，保证流程可测。
 */
@Slf4j
@Component
public class DingTalkNotifier {

    private static final OkHttpClient HTTP_CLIENT = new OkHttpClient();
    private static final MediaType JSON_TYPE = MediaType.get("application/json; charset=utf-8");
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Resource
    private DingTalkConfig dingTalkConfig;

    /**
     * 发送文本消息。
     *
     * @return true 表示发送成功（演示模式下始终 true）
     */
    public boolean sendText(String title, String content) {
        String webhook = dingTalkConfig.getNotifyWebhook();
        if (dingTalkConfig.isMockEnabled() || webhook == null || webhook.trim().isEmpty()) {
            log.info("[钉钉通知-演示模式] title={} content={}", title, content);
            return true;
        }
        try {
            ObjectNode body = MAPPER.createObjectNode();
            body.put("msgtype", "markdown");
            ObjectNode markdown = body.putObject("markdown");
            markdown.put("title", title);
            markdown.put("text", "### " + title + "\n\n" + content);
            Request request = new Request.Builder()
                    .url(webhook)
                    .post(RequestBody.create(body.toString(), JSON_TYPE))
                    .build();
            try (Response response = HTTP_CLIENT.newCall(request).execute()) {
                String resp = response.body() == null ? "" : response.body().string();
                log.info("钉钉通知响应: {}", resp);
                return response.isSuccessful();
            }
        } catch (Exception e) {
            log.error("钉钉通知发送失败", e);
            return false;
        }
    }
}
