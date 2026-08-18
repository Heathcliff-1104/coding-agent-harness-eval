package com.koolearn.bms.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.koolearn.bms.entity.User;
import com.koolearn.bms.mapper.UserMapper;
import com.koolearn.bms.util.dingtalk.DingTalkConfig;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class DingTalkLoginService {

    private static final OkHttpClient HTTP_CLIENT = new OkHttpClient();
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final MediaType JSON_TYPE = MediaType.get("application/json; charset=utf-8");
    private static final String AUTH_URL = "https://login.dingtalk.com/oauth2/auth";
    private static final String TOKEN_URL = "https://api.dingtalk.com/v1.0/oauth2/userAccessToken";
    private static final String USER_INFO_URL = "https://api.dingtalk.com/v1.0/contact/users/me";
    private static final ConcurrentHashMap<String, StateEntry> STATE_STORE = new ConcurrentHashMap<>();
    private static final long STATE_TTL_MS = 10 * 60 * 1000;

    @Resource
    private DingTalkConfig dingTalkConfig;
    @Resource
    private UserMapper userMapper;
    @Resource
    private PasswordEncoder passwordEncoder;

    public String buildAuthUrl(String redirectUri) {
        String state = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(UUID.randomUUID().toString().getBytes());
        // state 绑定 redirectUri，防止攻击者劫持授权回调
        STATE_STORE.put(state, new StateEntry(redirectUri, System.currentTimeMillis() + STATE_TTL_MS));
        cleanExpiredStates();

        if (dingTalkConfig.isMockEnabled()) {
            // 演示模式：返回本页回调地址（前端用任意code+state即可完成模拟登录）
            return "/login?state=" + state + "&code=demo";
        }

        return AUTH_URL + "?redirect_uri=" + urlEncode(redirectUri)
                + "&response_type=code"
                + "&client_id=" + urlEncode(dingTalkConfig.getAppKey())
                + "&scope=openid"
                + "&state=" + state
                + "&prompt=consent";
    }

    public User loginByCode(String code, String state) throws Exception {
        if (state == null) {
            throw new RuntimeException("无效的登录请求(state校验失败)");
        }
        StateEntry entry = STATE_STORE.remove(state);
        if (entry == null || entry.expireAt < System.currentTimeMillis()) {
            throw new RuntimeException("无效的登录请求(state过期或不存在)");
        }

        if (dingTalkConfig.isMockEnabled()) {
            // 演示模式：不调用真实钉钉，code 即模拟 unionId
            String mockUnionId = "mock-" + (code == null ? "demo" : code);
            User user = findOrCreateUser(mockUnionId, "演示钉钉用户");
            log.info("[演示模式] 钉钉扫码登录（模拟）: code={} -> {}", code, user.getUsername());
            user.setPassword(null);
            return user;
        }

        String userAccessToken = exchangeCodeForToken(code);
        ObjectNode userInfo = fetchDingTalkUserInfo(userAccessToken);
        String unionId = userInfo.get("unionId").asText();
        String nick = userInfo.has("nick") ? userInfo.get("nick").asText() : "钉钉用户";

        User user = findOrCreateUser(unionId, nick);
        user.setPassword(null);
        return user;
    }

    private String exchangeCodeForToken(String code) throws IOException {
        ObjectNode body = MAPPER.createObjectNode();
        body.put("clientId", dingTalkConfig.getAppKey());
        body.put("clientSecret", dingTalkConfig.getAppSecret());
        body.put("code", code);
        body.put("grantType", "authorization_code");

        Request request = new Request.Builder()
                .url(TOKEN_URL)
                .post(RequestBody.create(body.toString(), JSON_TYPE))
                .build();

        try (Response response = HTTP_CLIENT.newCall(request).execute()) {
            String resp = response.body().string();
            log.info("钉钉userAccessToken响应: {}", resp);
            ObjectNode json = (ObjectNode) MAPPER.readTree(resp);
            if (!json.has("accessToken")) {
                throw new RuntimeException("获取钉钉用户Token失败: " + resp);
            }
            return json.get("accessToken").asText();
        }
    }

    private ObjectNode fetchDingTalkUserInfo(String accessToken) throws IOException {
        Request request = new Request.Builder()
                .url(USER_INFO_URL)
                .addHeader("x-acs-dingtalk-access-token", accessToken)
                .build();

        try (Response response = HTTP_CLIENT.newCall(request).execute()) {
            String resp = response.body().string();
            log.info("钉钉用户信息响应: {}", resp);
            return (ObjectNode) MAPPER.readTree(resp);
        }
    }

    private User findOrCreateUser(String unionId, String nick) {
        User exist = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getDingtalkUnionId, unionId));
        if (exist != null) return exist;

        User newUser = new User();
        newUser.setUsername("dd_" + unionId.substring(Math.max(0, unionId.length() - 10)));
        newUser.setRealName(nick);
        newUser.setDingtalkUnionId(unionId);
        newUser.setRole("engineer");
        newUser.setStatus(1);
        newUser.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        userMapper.insert(newUser);
        log.info("钉钉新用户自动注册: {} -> {}", nick, newUser.getUsername());
        return newUser;
    }

    private void cleanExpiredStates() {
        long now = System.currentTimeMillis();
        STATE_STORE.entrySet().removeIf(e -> e.getValue() == null || e.getValue().expireAt < now);
    }

    private String urlEncode(String value) {
        try {
            return java.net.URLEncoder.encode(value, "UTF-8");
        } catch (Exception e) {
            return value;
        }
    }

    private static class StateEntry {
        final String redirectUri;
        final long expireAt;
        StateEntry(String redirectUri, long expireAt) {
            this.redirectUri = redirectUri;
            this.expireAt = expireAt;
        }
    }
}
