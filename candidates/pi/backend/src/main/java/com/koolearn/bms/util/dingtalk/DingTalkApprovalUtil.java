package com.koolearn.bms.util.dingtalk;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Component;
import javax.annotation.Resource;

@Slf4j
@Component
public class DingTalkApprovalUtil {
    private static final OkHttpClient HTTP_CLIENT = new OkHttpClient();
    private static final MediaType JSON_TYPE = MediaType.get("application/json; charset=utf-8");
    private static final String APPROVAL_URL = "https://api.dingtalk.com/v1.0/workflow/processInstances";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Resource
    private DingTalkTokenUtil dingTalkTokenUtil;
    @Resource
    private DingTalkConfig dingTalkConfig;

    public String createInboundApproval(String billNo, String tableJson) throws Exception {
        String accessToken = dingTalkTokenUtil.getAccessToken();
        log.info("获取到accessToken: {}", accessToken.substring(0, Math.min(10, accessToken.length())) + "...");

        ArrayNode formValues = MAPPER.createArrayNode();
        formValues.add(buildFormItem("入库单号", billNo));
        formValues.add(buildFormItem("物料明细", tableJson));

        ObjectNode body = MAPPER.createObjectNode();
        body.put("processCode", dingTalkConfig.getInboundProcessCode());
        body.put("originatorUserId", dingTalkConfig.getOriginatorUserId());
        body.put("deptId", dingTalkConfig.getDeptId());
        body.put("title", "入库审批单-" + billNo);
        body.set("formComponentValues", formValues);

        log.info("发起入库审批: {}", body.toString());

        RequestBody requestBody = RequestBody.create(body.toString(), JSON_TYPE);
        Request request = new Request.Builder()
                .url(APPROVAL_URL)
                .addHeader("x-acs-dingtalk-access-token", accessToken)
                .post(requestBody)
                .build();

        try (Response response = HTTP_CLIENT.newCall(request).execute()) {
            String result = response.body().string();
            log.info("入库审批响应: {}", result);
            ObjectNode json = (ObjectNode) MAPPER.readTree(result);
            if (!json.has("instanceId")) {
                throw new RuntimeException("发起入库审批失败：" + result);
            }
            return json.get("instanceId").asText();
        }
    }

    public String createOutboundApproval(String outboundCode, Integer outType,
                                         String applyUser, String remark, String detailJson) throws Exception {
        String accessToken = dingTalkTokenUtil.getAccessToken();

        ArrayNode formValues = MAPPER.createArrayNode();
        formValues.add(buildFormItem("出库单号", outboundCode));
        formValues.add(buildFormItem("出库类型", outType == 1 ? "生产领料" : "其他出库"));
        formValues.add(buildFormItem("申请人", applyUser));
        formValues.add(buildFormItem("备注", remark == null ? "" : remark));
        formValues.add(buildFormItem("出库物料明细", detailJson));

        ObjectNode body = MAPPER.createObjectNode();
        body.put("processCode", dingTalkConfig.getOutboundProcessCode());
        body.put("originatorUserId", dingTalkConfig.getOriginatorUserId());
        body.put("deptId", dingTalkConfig.getDeptId());
        body.put("title", "出库审批单-" + outboundCode);
        body.set("formComponentValues", formValues);

        log.info("发起出库审批: {}", body.toString());

        RequestBody requestBody = RequestBody.create(body.toString(), JSON_TYPE);
        Request request = new Request.Builder()
                .url(APPROVAL_URL)
                .addHeader("x-acs-dingtalk-access-token", accessToken)
                .post(requestBody)
                .build();

        try (Response response = HTTP_CLIENT.newCall(request).execute()) {
            String result = response.body().string();
            log.info("出库审批响应: {}", result);
            ObjectNode json = (ObjectNode) MAPPER.readTree(result);
            if (!json.has("instanceId")) {
                throw new RuntimeException("发起出库审批失败：" + result);
            }
            return json.get("instanceId").asText();
        }
    }

    private ObjectNode buildFormItem(String name, String value) {
        ObjectNode item = MAPPER.createObjectNode();
        item.put("name", name);
        item.put("value", value);
        return item;
    }
}
