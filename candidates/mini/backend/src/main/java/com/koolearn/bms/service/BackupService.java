package com.koolearn.bms.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.koolearn.bms.entity.SysBackupRecord;

public interface BackupService {
    String backup(String type);
    IPage<SysBackupRecord> listRecords(Long pageNum, Long pageSize);
    void cleanupExpired(int retentionDays);
}
