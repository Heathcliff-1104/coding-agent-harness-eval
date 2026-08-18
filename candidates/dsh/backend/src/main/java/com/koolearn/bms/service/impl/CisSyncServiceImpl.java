package com.koolearn.bms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.koolearn.bms.entity.CisSyncLog;
import com.koolearn.bms.entity.Material;
import com.koolearn.bms.mapper.CisSyncLogMapper;
import com.koolearn.bms.mapper.MaterialMapper;
import com.koolearn.bms.service.CisSyncService;
import com.koolearn.bms.util.dingtalk.DingTalkConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * CIS 同步实现：
 * - 配置 cis.endpoint 且非演示模式时，通过 HTTP 上报物料数据到 CIS 系统。
 * - 演示模式（默认）下生成确定性的模拟同步日志，保证功能可测。
 */
@Slf4j
@Service
public class CisSyncServiceImpl implements CisSyncService {

    private final MaterialMapper materialMapper;
    private final CisSyncLogMapper syncLogMapper;
    private final DingTalkConfig dingTalkConfig;

    public CisSyncServiceImpl(MaterialMapper materialMapper, CisSyncLogMapper syncLogMapper,
                              DingTalkConfig dingTalkConfig) {
        this.materialMapper = materialMapper;
        this.syncLogMapper = syncLogMapper;
        this.dingTalkConfig = dingTalkConfig;
    }

    @Override
    public String syncFull() {
        List<Material> materials = materialMapper.selectList(null);
        return doSync("full", materials);
    }

    @Override
    public String syncIncremental() {
        LocalDateTime since = LocalDateTime.now().minusDays(7);
        List<Material> materials = materialMapper.selectList(
                new LambdaQueryWrapper<Material>().ge(Material::getUpdateTime, since));
        return doSync("incremental", materials);
    }

    private String doSync(String type, List<Material> materials) {
        CisSyncLog record = new CisSyncLog();
        record.setSyncType(type);
        record.setMaterialCount(materials.size());
        record.setCreateTime(LocalDateTime.now());
        try {
            String result = sendToCis(type, materials);
            record.setSyncStatus("success");
            record.setMessage("同步成功，共 " + materials.size() + " 条" + (result != null ? "：" + result : ""));
            syncLogMapper.insert(record);
            log.info("CIS[{}]同步成功: {} 条", type, materials.size());
            return "同步成功，共 " + materials.size() + " 条物料";
        } catch (Exception e) {
            record.setSyncStatus("failed");
            record.setMessage("同步失败: " + e.getMessage());
            syncLogMapper.insert(record);
            log.error("CIS[{}]同步失败", type, e);
            return "同步失败: " + e.getMessage();
        }
    }

    private String sendToCis(String type, List<Material> materials) throws Exception {
        if (dingTalkConfig.isMockEnabled()) {
            // 演示模式：不调用真实CIS，返回确定性的模拟结果
            int lowStock = (int) materials.stream().filter(m -> m.getMinStock() != null
                    && m.getStock() != null && m.getStock().compareTo(m.getMinStock()) < 0).count();
            return "演示模式（未配置CIS endpoint），低库存物料 " + lowStock + " 条";
        }
        String endpoint = dingTalkConfig.getCisEndpoint();
        if (endpoint == null || endpoint.trim().isEmpty()) {
            throw new RuntimeException("未配置 cis.endpoint，无法同步CIS");
        }
        // 真实CIS上报（按需实现具体协议；此处构造JSON并POST）
        okhttp3.OkHttpClient client = new okhttp3.OkHttpClient();
        com.fasterxml.jackson.databind.node.ArrayNode arr = new com.fasterxml.jackson.databind.ObjectMapper().createArrayNode();
        for (Material m : materials) {
            com.fasterxml.jackson.databind.node.ObjectNode node = arr.addObject();
            node.put("materialCode", m.getMaterialCode());
            node.put("materialName", m.getMaterialName());
            node.put("packageType", m.getPackageType());
            node.put("valueData", m.getValueData());
            node.put("specModel", m.getSpecModel());
            node.put("stock", m.getStock() == null ? 0 : m.getStock().doubleValue());
            node.put("lockStock", m.getLockStock() == null ? 0 : m.getLockStock().doubleValue());
            node.put("minStock", m.getMinStock() == null ? 0 : m.getMinStock().doubleValue());
            node.put("maxStock", m.getMaxStock() == null ? 0 : m.getMaxStock().doubleValue());
        }
        okhttp3.RequestBody body = okhttp3.RequestBody.create(arr.toString(),
                okhttp3.MediaType.get("application/json; charset=utf-8"));
        okhttp3.Request request = new okhttp3.Request.Builder().url(endpoint).post(body).build();
        try (okhttp3.Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new RuntimeException("CIS HTTP " + response.code());
            }
            return response.body() == null ? "" : response.body().string();
        }
    }

    @Override
    public IPage<CisSyncLog> pageLogs(Long pageNum, Long pageSize) {
        Page<CisSyncLog> page = new Page<>(pageNum, pageSize);
        return syncLogMapper.selectPage(page, new LambdaQueryWrapper<CisSyncLog>().orderByDesc(CisSyncLog::getCreateTime));
    }
}
