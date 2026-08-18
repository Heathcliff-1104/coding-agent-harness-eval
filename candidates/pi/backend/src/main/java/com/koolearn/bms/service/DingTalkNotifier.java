package com.koolearn.bms.service;

import java.util.List;

/**
 * 钉钉消息通知器：dingtalk.mode=mock 或 webhook 为空时仅记录日志/操作日志，
 * 否则推送机器人 webhook。
 */
public interface DingTalkNotifier {

    void send(String title, String content, List<String> targets);
}
