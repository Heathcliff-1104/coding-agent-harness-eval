package com.koolearn.bms.util.dingtalk;

import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.net.URLEncoder;

/**
 * 钉钉群机器人通知。
 * 未配置 webhook 时进入演示模式：仅打印日志，不发起真实网络调用。
 */
@Slf4j
@Component
public class DingTalkNotifier {

    @Value("${dingtalk.robot.webhook:}")
    private String webhook;

    @Value("${dingtalk.robot.secret:}")
    private String secret;

    private static final OkHttpClient HTTP_CLIENT = new OkHttpClient();
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    public boolean isEnabled() {
        return webhook != null && !webhook.trim().isEmpty();
    }

    public void sendText(String content) {
        if (!isEnabled()) {
            log.info("[钉钉机器人-演示模式] {}", content);
            return;
        }
        try {
            String url = webhook;
            if (secret != null && !secret.trim().isEmpty()) {
                long timestamp = System.currentTimeMillis();
                String stringToSign = timestamp + "\n" + secret;
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                byte[] digest = md.digest(stringToSign.getBytes(StandardCharsets.UTF_8));
                String sign = Base64.getEncoder().encodeToString(digest);
                String encodedSign = URLEncoder.encode(sign, "UTF-8");
                url = webhook + (webhook.contains("?") ? "&" : "?") + "timestamp=" + timestamp + "&sign=" + encodedSign;
            }
            String contentValue = content == null ? "" : escapeJson(content);
            String body = "{\"msgtype\":\"text\",\"text\":{\"content\":\"" + contentValue + "\"}}";
            Request request = new Request.Builder()
                    .url(url)
                    .post(RequestBody.create(body, JSON))
                    .build();
            try (Response response = HTTP_CLIENT.newCall(request).execute()) {
                log.info("[钉钉机器人] 发送结果: {}", response.body() == null ? "" : response.body().string());
            }
        } catch (Exception e) {
            log.warn("[钉钉机器人] 发送失败: {}", e.getMessage());
        }
    }

    private String escapeJson(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
