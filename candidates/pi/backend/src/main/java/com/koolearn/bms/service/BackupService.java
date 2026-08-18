package com.koolearn.bms.service;

/**
 * 数据库备份服务
 */
public interface BackupService {

    /**
     * 执行一次备份
     * @param type full=全量 incremental=增量（简化实现：同 mysqldump，标注为增量）
     * @return 备份结果描述
     */
    String backup(String type);
}
