package com.koolearn.bms.util.dingtalk;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 钉钉审批回调验签工具。
 *
 * 当配置了 dingtalk.callback.secret 时，回调请求必须携带
 * X-DingTalk-Signature = Base64(HMAC-SHA256(instanceId|result|timestamp, secret))
 * 否则拒绝处理。未配置密钥时视为演示模式，仅记录警告。
 */
@Component
public class DingTalkCallbackVerifier {

    @Value("${dingtalk.callback.secret:}")
    private String callbackSecret;

    /**
     * 校验回调签名。secret 未配置时（演示模式）返回 true 但记警告由调用方处理。
     */
    public boolean verify(String instanceId, String result, String timestamp, String signature) {
        if (callbackSecret == null || callbackSecret.trim().isEmpty()) {
            return true;
        }
        if (signature == null || signature.trim().isEmpty()) {
            return false;
        }
        try {
            String payload = String.join("|",
                    nullToEmpty(instanceId), nullToEmpty(result), nullToEmpty(timestamp));
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(callbackSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String expected = Base64.getEncoder().encodeToString(
                    mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
            return constantTimeEquals(expected, signature.trim());
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isDemoMode() {
        return callbackSecret == null || callbackSecret.trim().isEmpty();
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null || a.length() != b.length()) return false;
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }
}
