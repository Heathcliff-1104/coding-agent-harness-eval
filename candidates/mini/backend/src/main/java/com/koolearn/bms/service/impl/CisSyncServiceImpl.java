package com.koolearn.bms.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.koolearn.bms.entity.Material;
import com.koolearn.bms.entity.SysCisSyncLog;
import com.koolearn.bms.mapper.MaterialMapper;
import com.koolearn.bms.mapper.SysCisSyncLogMapper;
import com.koolearn.bms.service.CisSyncService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * CIS 元件库同步适配器。
 * 支持两种模式：
 *  - mock：不发起真实网络调用，仅记录同步日志（确定性本地演示）
 *  - http：将物料数据 POST 到配置的 CIS 服务 base-url
 * 由于外部 CIS 服务在评估环境中不可用，默认使用 mock 模式。
 */
@Slf4j
@Service
public class CisSyncServiceImpl extends ServiceImpl<SysCisSyncLogMapper, SysCisSyncLog> implements CisSyncService {

    @Value("${cis.adapter.mode:mock}")
    private String mode;

    @Value("${cis.adapter.base-url:http://localhost:9080}")
    private String baseUrl;

    @Value("${cis.adapter.token:}")
    private String token;

    private final MaterialMapper materialMapper;

    public CisSyncServiceImpl(MaterialMapper materialMapper) {
        this.materialMapper = materialMapper;
    }

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final OkHttpClient HTTP_CLIENT = new OkHttpClient();
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    @Override
    public String sync(String type) {
        List<Material> materials = materialMapper.selectList(null);
        int success = 0;
        int fail = 0;
        List<Map<String, Object>> payload = new ArrayList<>();
        for (Material m : materials) {
            try {
                Map<String, Object> item = new java.util.HashMap<>();
                item.put("materialCode", m.getMaterialCode());
                item.put("materialName", m.getMaterialName());
                item.put("packageType", m.getPackageType());
                item.put("valueData", m.getValueData());
                item.put("specModel", m.getSpecModel());
                item.put("stock", m.getStock());
                item.put("batchNo", m.getUpdateTime() == null ? "" : m.getUpdateTime().toString());
                payload.add(item);
                success++;
            } catch (Exception e) {
                fail++;
            }
        }

        String status = "SUCCESS";
        String errorMsg = "";
        try {
            if ("http".equalsIgnoreCase(mode) && baseUrl != null && !baseUrl.trim().isEmpty()) {
                RequestBody body = RequestBody.create(MAPPER.writeValueAsString(payload), JSON);
                Request request = new Request.Builder()
                        .url(baseUrl + "/api/cis/materials/sync")
                        .header("Authorization", token == null ? "" : token)
                        .post(body)
                        .build();
                try (Response response = HTTP_CLIENT.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        status = "FAILED";
                        errorMsg = "HTTP " + response.code() + ": " + (response.body() == null ? "" : response.body().string());
                    }
                }
            } else {
                log.info("[CIS-演示模式] 全量同步 {} 条物料，不发起真实网络调用", payload.size());
            }
        } catch (Exception e) {
            status = "FAILED";
            errorMsg = e.getMessage();
            log.error("CIS 同步异常", e);
        }

        SysCisSyncLog logEntry = new SysCisSyncLog();
        logEntry.setSyncType(type);
        logEntry.setSyncMode("http".equalsIgnoreCase(mode) ? "http" : "mock");
        logEntry.setTotalCount(materials.size());
        logEntry.setSuccessCount(success);
        logEntry.setFailCount(fail);
        logEntry.setStatus(status);
        logEntry.setErrorMsg(errorMsg);
        logEntry.setCreateTime(LocalDateTime.now());
        save(logEntry);
        return status.equals("SUCCESS") ? "同步完成，共" + materials.size() + "条，成功" + success + "条" : "同步失败: " + errorMsg;
    }
}
