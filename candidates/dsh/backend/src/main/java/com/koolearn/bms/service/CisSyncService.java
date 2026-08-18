package com.koolearn.bms.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.koolearn.bms.entity.CisSyncLog;

/**
 * 同步CIS元件库（需求 2.4.3）：物料编码、封装、值、库存数量、批次信息同步至 CIS 系统。
 * 支持手动触发全量同步与增量同步。
 */
public interface CisSyncService {

    /** 全量同步所有物料。 */
    String syncFull();

    /** 增量同步（近N天变更的物料）。 */
    String syncIncremental();

    IPage<CisSyncLog> pageLogs(Long pageNum, Long pageSize);
}
