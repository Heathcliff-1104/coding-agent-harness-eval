package com.koolearn.bms.util.dingtalk;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.*;
import org.springframework.stereotype.Component;
import javax.annotation.Resource;
import java.io.IOException;

@Component
public class DingTalkTokenUtil {
    private volatile String accessToken;
    private volatile long expireTime;
    private static final long TTL = 7200 * 1000;
    private static final OkHttpClient HTTP_CLIENT = new OkHttpClient();
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final MediaType JSON_TYPE = MediaType.get("application/json; charset=utf-8");

    @Resource
    private DingTalkConfig dingTalkConfig;

    public synchronized String getAccessToken() throws Exception {
        long now = System.currentTimeMillis();
        if (accessToken != null && now < expireTime) {
            return accessToken;
        }

        ObjectNode reqBody = MAPPER.createObjectNode();
        reqBody.put("appKey", dingTalkConfig.getAppKey());
        reqBody.put("appSecret", dingTalkConfig.getAppSecret());

        RequestBody body = RequestBody.create(reqBody.toString(), JSON_TYPE);
        Request request = new Request.Builder()
                .url("https://api.dingtalk.com/v1.0/oauth2/accessToken")
                .post(body)
                .build();

        try (Response response = HTTP_CLIENT.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("请求Token接口失败, HTTP " + response.code());
            }
            String respBody = response.body().string();
            ObjectNode json = (ObjectNode) MAPPER.readTree(respBody);
            if (json.has("accessToken")) {
                accessToken = json.get("accessToken").asText();
                expireTime = now + TTL - 30000;
                return accessToken;
            }
            throw new RuntimeException("获取Token失败：" + respBody);
        }
    }
}
