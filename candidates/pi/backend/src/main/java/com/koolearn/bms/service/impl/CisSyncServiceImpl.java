package com.koolearn.bms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.koolearn.bms.entity.CisSyncLog;
import com.koolearn.bms.entity.Material;
import com.koolearn.bms.mapper.CisSyncLogMapper;
import com.koolearn.bms.service.CisSyncService;
import com.koolearn.bms.service.MaterialService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CisSyncServiceImpl implements CisSyncService {

    private static final OkHttpClient CLIENT = new OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build();
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final MaterialService materialService;
    private final CisSyncLogMapper cisSyncLogMapper;

    @Value("${cis.mode:mock}")
    private String mode;

    @Value("${cis.url:}")
    private String cisUrl;

    public CisSyncServiceImpl(MaterialService materialService, CisSyncLogMapper cisSyncLogMapper) {
        this.materialService = materialService;
        this.cisSyncLogMapper = cisSyncLogMapper;
    }

    @Override
    public void syncFull() {
        List<Material> materials = materialService.listAll();
        doSync("full", materials);
    }

    @Override
    public void syncIncremental() {
        // 以上次成功同步时间为界，同步更新的物料；无记录则同步全部
        CisSyncLog last = cisSyncLogMapper.selectList(new LambdaQueryWrapper<CisSyncLog>()
                        .eq(CisSyncLog::getStatus, "SUCCESS")
                        .orderByDesc(CisSyncLog::getSyncTime).last("limit 1"))
                .stream().findFirst().orElse(null);
        List<Material> materials;
        if (last != null && last.getSyncTime() != null) {
            materials = materialService.list(new LambdaQueryWrapper<Material>()
                    .ge(Material::getUpdateTime, last.getSyncTime()));
        } else {
            materials = materialService.listAll();
        }
        doSync("incremental", materials);
    }

    private void doSync(String type, List<Material> materials) {
        if (materials == null) {
            materials = java.util.Collections.emptyList();
        }
        CisSyncLog logRow = new CisSyncLog();
        logRow.setSyncType(type);
        logRow.setRows(materials.size());
        logRow.setSyncTime(LocalDateTime.now());
        try {
            if ("mock".equals(mode) || !StringUtils.hasText(cisUrl)) {
                log.info("[CIS mock同步] type={} rows={}", type, materials.size());
                logRow.setStatus("SUCCESS");
                logRow.setMessage("mock模式，未调用外部CIS服务");
                cisSyncLogMapper.insert(logRow);
                return;
            }
            ObjectNode payload = MAPPER.createObjectNode();
            payload.put("type", type);
            ArrayNode arr = payload.putArray("materials");
            for (Material m : materials) {
                ObjectNode node = arr.addObject();
                node.put("materialCode", nvl(m.getMaterialCode()));
                node.put("packageType", nvl(m.getPackageType()));
                node.put("valueData", nvl(m.getValueData()));
                node.put("stock", m.getStock() != null ? m.getStock() : java.math.BigDecimal.ZERO);
                node.put("batchNo", nvl(m.getManufacturerBatch()));
                node.put("locationNo", nvl(m.getLocationNo()));
                node.put("manufacturerName", nvl(m.getManufacturerName()));
            }
            Request request = new Request.Builder()
                    .url(cisUrl)
                    .post(RequestBody.create(payload.toString(), JSON))
                    .build();
            try (Response response = CLIENT.newCall(request).execute()) {
                String body = response.body() != null ? response.body().string() : "";
                if (response.isSuccessful()) {
                    logRow.setStatus("SUCCESS");
                    logRow.setMessage("CIS同步成功 rows=" + materials.size());
                } else {
                    logRow.setStatus("FAILED");
                    logRow.setMessage("CIS同步失败 code=" + response.code() + " body=" + truncate(body));
                }
            }
        } catch (Exception e) {
            log.warn("CIS同步异常: {}", e.getMessage());
            logRow.setStatus("FAILED");
            logRow.setMessage("CIS同步异常: " + e.getMessage());
        }
        cisSyncLogMapper.insert(logRow);
    }

    private String nvl(String s) {
        return s != null ? s : "";
    }

    private String truncate(String s) {
        if (s == null) return "";
        return s.length() > 200 ? s.substring(0, 200) : s;
    }
}
