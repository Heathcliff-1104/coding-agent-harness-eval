package com.koolearn.bms.service.impl;

import com.koolearn.bms.service.DingTalkNotifier;
import com.koolearn.bms.service.SysOperationLogService;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class DingTalkNotifierImpl implements DingTalkNotifier {

    private static final OkHttpClient CLIENT = new OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build();
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final SysOperationLogService sysLogService;

    @Value("${dingtalk.mode:mock}")
    private String mode;

    @Value("${dingtalk.webhook.url:}")
    private String webhookUrl;

    public DingTalkNotifierImpl(SysOperationLogService sysLogService) {
        this.sysLogService = sysLogService;
    }

    @Override
    public void send(String title, String content, List<String> targets) {
        String targetsStr = targets == null ? "" : String.join(",", targets);
        try {
            if ("robot".equals(mode) && StringUtils.hasText(webhookUrl)) {
                String body = buildRobotJson(title, content, targets);
                Request request = new Request.Builder()
                        .url(webhookUrl)
                        .post(RequestBody.create(body, JSON))
                        .build();
                try (Response response = CLIENT.newCall(request).execute()) {
                    if (response.isSuccessful()) {
                        log.info("钉钉通知发送成功: {} -> {}", title, targetsStr);
                    } else {
                        log.warn("钉钉通知发送失败: code={} body={}", response.code(), response.body() != null ? response.body().string() : "");
                    }
                }
            } else {
                log.info("[钉钉mock通知] {} | {} | 收件人:{}", title, content, targetsStr);
                sysLogService.log("system", "钉钉通知", title + ": " + content + "（收件人:" + targetsStr + "）", "internal");
            }
        } catch (Exception e) {
            log.warn("钉钉通知异常: {}", e.getMessage());
            sysLogService.log("system", "钉钉通知", title + " 发送失败: " + e.getMessage(), "internal");
        }
    }

    private String buildRobotJson(String title, String content, List<String> targets) {
        StringBuilder at = new StringBuilder();
        if (targets != null && !targets.isEmpty()) {
            for (String t : targets) {
                at.append("\"").append(t).append("\",");
            }
        }
        String text = "### " + title + "\n" + content;
        return "{\"msgtype\":\"markdown\",\"markdown\":{\"title\":\"" + esc(title)
                + "\",\"text\":\"" + esc(text) + "\"},\"at\":{\"atMobiles\":[" + at + "]}}";
    }

    private String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    }
}
