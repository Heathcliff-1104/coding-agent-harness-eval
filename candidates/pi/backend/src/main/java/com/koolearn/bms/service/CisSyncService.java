package com.koolearn.bms.service;

/**
 * CIS 元件库同步服务
 */
public interface CisSyncService {

    /** 全量同步所有物料 */
    void syncFull();

    /** 增量同步（上次同步后更新的物料；无历史记录时同步全部） */
    void syncIncremental();
}
