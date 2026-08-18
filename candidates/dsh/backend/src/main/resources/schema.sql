-- =============================================================
-- 物料管理系统 (BMS) 数据库初始化脚本
-- MySQL 8 / InnoDB / utf8mb4
-- 与 backend 实体、Mapper XML、@Select 语句逐条对齐
-- =============================================================
CREATE DATABASE IF NOT EXISTS bms DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE bms;

-- ============ 1. 用户 ============
CREATE TABLE IF NOT EXISTS sys_user (
  id                BIGINT       NOT NULL AUTO_INCREMENT,
  username          VARCHAR(64)  NOT NULL,
  password          VARCHAR(100) NOT NULL,
  real_name         VARCHAR(64)  DEFAULT NULL,
  phone             VARCHAR(20)  DEFAULT NULL,
  dept              VARCHAR(64)  DEFAULT NULL,
  role              VARCHAR(32)  NOT NULL DEFAULT 'engineer' COMMENT '主角色 admin/warehouse/engineer/purchaser/inspector/manager 或自定义角色code',
  status            TINYINT      NOT NULL DEFAULT 1 COMMENT '1启用 0禁用',
  dingtalk_union_id VARCHAR(128) DEFAULT NULL,
  last_login_time   DATETIME     DEFAULT NULL COMMENT '上次登录时间',
  create_time       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_username (username),
  UNIQUE KEY uk_phone (phone),
  UNIQUE KEY uk_dingtalk_union_id (dingtalk_union_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户';

-- ============ 2. 物料 ============
CREATE TABLE IF NOT EXISTS tb_material (
  id              BIGINT        NOT NULL AUTO_INCREMENT,
  material_code   VARCHAR(64)   DEFAULT NULL COMMENT '可为空，确认入库时自动生成',
  material_name   VARCHAR(128)  NOT NULL,
  package_type    VARCHAR(64)   DEFAULT NULL COMMENT '封装',
  value_data      VARCHAR(64)   DEFAULT NULL COMMENT 'value值',
  spec_model      VARCHAR(128)  DEFAULT NULL COMMENT '规格型号',
  manufacturer    VARCHAR(128)  DEFAULT NULL COMMENT '厂家名称',
  warehouse_code  VARCHAR(64)   DEFAULT NULL,
  location_no     VARCHAR(64)   DEFAULT NULL COMMENT '库位',
  remark          VARCHAR(512)  DEFAULT NULL,
  stock           DECIMAL(18,4) NOT NULL DEFAULT 0 COMMENT '当前总库存',
  lock_stock      DECIMAL(18,4) NOT NULL DEFAULT 0 COMMENT '锁定占用库存',
  min_stock       DECIMAL(18,4) DEFAULT NULL COMMENT '最低预警库存',
  max_stock       DECIMAL(18,4) DEFAULT NULL COMMENT '最高超储库存',
  stagnation_days INT           DEFAULT 90 COMMENT '呆滞判定天数',
  material_cost   DECIMAL(12,4) DEFAULT 0 COMMENT '单件成本',
  version         INT           NOT NULL DEFAULT 0 COMMENT '乐观锁版本号，必须NOT NULL DEFAULT 0',
  create_time     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_material_code (material_code),
  KEY idx_material_name (material_name),
  KEY idx_warehouse_code (warehouse_code),
  CONSTRAINT chk_stock      CHECK (stock >= 0),
  CONSTRAINT chk_lock_stock CHECK (lock_stock >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='物料';

-- ============ 3. 入库单 ============
CREATE TABLE IF NOT EXISTS tb_inbound_order (
  id               BIGINT       NOT NULL AUTO_INCREMENT,
  bill_no          VARCHAR(64)  NOT NULL,
  in_type          TINYINT      NOT NULL DEFAULT 1 COMMENT '1采购入库 2退库入库',
  return_reason    VARCHAR(255) DEFAULT NULL COMMENT '退库原因',
  supplier         VARCHAR(128) DEFAULT NULL,
  apply_user       VARCHAR(64)  DEFAULT NULL,
  apply_time       DATETIME     DEFAULT NULL,
  in_date          DATETIME     DEFAULT NULL,
  remark           VARCHAR(512) DEFAULT NULL,
  ding_instance_id VARCHAR(128) DEFAULT NULL,
  order_status     TINYINT      NOT NULL DEFAULT 0 COMMENT '0待审批 1已入库 2已拒绝',
  create_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_ding_instance (ding_instance_id),
  KEY idx_bill_no (bill_no),
  KEY idx_order_status (order_status),
  KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='入库单';

-- ============ 4. 入库单明细 ============
CREATE TABLE IF NOT EXISTS in_storage_item (
  id            BIGINT        NOT NULL AUTO_INCREMENT,
  inbound_id    BIGINT        NOT NULL,
  material_id   BIGINT        DEFAULT NULL,
  material_code VARCHAR(64)   DEFAULT NULL,
  material_name VARCHAR(128)  DEFAULT NULL,
  package_type  VARCHAR(64)   DEFAULT NULL,
  value_data    VARCHAR(64)   DEFAULT NULL,
  spec_model    VARCHAR(128)  DEFAULT NULL,
  manufacturer  VARCHAR(128)  DEFAULT NULL,
  num           DECIMAL(18,4) NOT NULL,
  batch_no      VARCHAR(64)   DEFAULT NULL,
  location_no   VARCHAR(64)   DEFAULT NULL,
  remark        VARCHAR(512)  DEFAULT NULL,
  create_time   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_inbound_id (inbound_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='入库单明细';

-- ============ 5. 出库单 ============
CREATE TABLE IF NOT EXISTS tb_outbound_order (
  id               BIGINT       NOT NULL AUTO_INCREMENT,
  outbound_code    VARCHAR(64)  NOT NULL,
  out_type         TINYINT      NOT NULL DEFAULT 1 COMMENT '1生产领料 2销售出库 3退货出库',
  apply_user       VARCHAR(64)  DEFAULT NULL,
  oper_user        VARCHAR(64)  DEFAULT NULL,
  remark           VARCHAR(512) DEFAULT NULL,
  order_status     TINYINT      NOT NULL DEFAULT 0 COMMENT '0待审批 1已出库 2已驳回',
  ding_instance_id VARCHAR(128) DEFAULT NULL,
  create_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_ding_instance (ding_instance_id),
  KEY idx_outbound_code (outbound_code),
  KEY idx_order_status (order_status),
  KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='出库单';

-- ============ 6. 出库单明细 ============
CREATE TABLE IF NOT EXISTS tb_out_storage_item (
  id            BIGINT        NOT NULL AUTO_INCREMENT,
  outbound_id   BIGINT        NOT NULL,
  material_id   BIGINT        DEFAULT NULL,
  material_code VARCHAR(64)   DEFAULT NULL,
  material_name VARCHAR(128)  DEFAULT NULL,
  batch_no      VARCHAR(64)   DEFAULT NULL,
  out_num       DECIMAL(18,4) NOT NULL,
  create_time   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_outbound_id (outbound_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='出库单明细';

-- ============ 7. 入库流水 ============
CREATE TABLE IF NOT EXISTS in_record (
  id            BIGINT        NOT NULL AUTO_INCREMENT,
  bill_no       VARCHAR(64)   DEFAULT NULL,
  material_id   BIGINT        DEFAULT NULL,
  material_code VARCHAR(64)   DEFAULT NULL,
  material_name VARCHAR(128)  DEFAULT NULL,
  batch_no      VARCHAR(64)   DEFAULT NULL,
  location_no   VARCHAR(64)   DEFAULT NULL,
  supplier      VARCHAR(128)  DEFAULT NULL COMMENT '供应商，用于按供应商统计',
  in_num        DECIMAL(18,4) NOT NULL,
  in_user       VARCHAR(64)   DEFAULT NULL,
  in_time       DATETIME      NOT NULL,
  create_time   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_in_time (in_time),
  KEY idx_bill_no (bill_no),
  KEY idx_material_id (material_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='入库流水';

-- ============ 8. 出库流水 ============
CREATE TABLE IF NOT EXISTS tb_out_record (
  id            BIGINT        NOT NULL AUTO_INCREMENT,
  outbound_code VARCHAR(64)   DEFAULT NULL,
  material_id   BIGINT        DEFAULT NULL,
  material_code VARCHAR(64)   DEFAULT NULL,
  material_name VARCHAR(128)  DEFAULT NULL,
  batch_no      VARCHAR(64)   DEFAULT NULL,
  dept          VARCHAR(64)   DEFAULT NULL COMMENT '领料部门，用于按部门统计',
  out_num       DECIMAL(18,4) NOT NULL,
  out_user      VARCHAR(64)   DEFAULT NULL,
  out_time      DATETIME      NOT NULL,
  PRIMARY KEY (id),
  KEY idx_out_time (out_time),
  KEY idx_material_id (material_id),
  KEY idx_outbound_code (outbound_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='出库流水';

-- ============ 9. 库存预警 ============
CREATE TABLE IF NOT EXISTS sys_stock_alert (
  id              BIGINT        NOT NULL AUTO_INCREMENT,
  material_id     BIGINT        DEFAULT NULL,
  material_code   VARCHAR(64)   DEFAULT NULL,
  material_name   VARCHAR(128)  DEFAULT NULL,
  alert_type      TINYINT       NOT NULL COMMENT '1低库存 2超储',
  current_stock   DECIMAL(18,4) DEFAULT NULL,
  threshold_stock DECIMAL(18,4) DEFAULT NULL,
  handled         TINYINT       NOT NULL DEFAULT 0 COMMENT '0未处理 1已处理',
  handler         VARCHAR(64)   DEFAULT NULL,
  handle_method   VARCHAR(128)  DEFAULT NULL,
  handle_time     DATETIME      DEFAULT NULL,
  notify_result   VARCHAR(255)  DEFAULT NULL COMMENT '钉钉通知结果',
  create_time     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_material_id (material_id),
  KEY idx_alert_type_handled (alert_type, handled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='库存预警';

-- ============ 10. 登录日志 ============
CREATE TABLE IF NOT EXISTS sys_login_log (
  id           BIGINT       NOT NULL AUTO_INCREMENT,
  username     VARCHAR(64)  DEFAULT NULL,
  login_ip     VARCHAR(64)  DEFAULT NULL,
  device_info  VARCHAR(512) DEFAULT NULL COMMENT 'User-Agent',
  login_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  login_result TINYINT      NOT NULL DEFAULT 0 COMMENT '0失败 1成功',
  PRIMARY KEY (id),
  KEY idx_username (username),
  KEY idx_login_time (login_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='登录日志';

-- ============ 11. 操作日志 ============
CREATE TABLE IF NOT EXISTS sys_operation_log (
  id          BIGINT        NOT NULL AUTO_INCREMENT,
  username    VARCHAR(64)   DEFAULT NULL,
  operation   VARCHAR(128)  DEFAULT NULL,
  description VARCHAR(1024) DEFAULT NULL,
  ip          VARCHAR(64)   DEFAULT NULL,
  result      VARCHAR(64)   DEFAULT '成功',
  create_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_username (username),
  KEY idx_operation (operation),
  KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志';

-- ============ 12. 备料计划单 (BOM) ============
CREATE TABLE IF NOT EXISTS bom_plan (
  id            BIGINT        NOT NULL AUTO_INCREMENT,
  plan_no       VARCHAR(64)   NOT NULL COMMENT '备料计划单号',
  bom_version   VARCHAR(64)   DEFAULT NULL COMMENT 'BOM版本，支持重复BOM匹配历史',
  material_id   BIGINT        DEFAULT NULL,
  material_code VARCHAR(64)   DEFAULT NULL,
  material_name VARCHAR(128)  DEFAULT NULL,
  package_type  VARCHAR(64)   DEFAULT NULL,
  value_data    VARCHAR(64)   DEFAULT NULL,
  spec_model    VARCHAR(128)  DEFAULT NULL,
  batch_no      VARCHAR(64)   DEFAULT NULL,
  need_num      DECIMAL(18,4) NOT NULL COMMENT '需要数量',
  current_stock DECIMAL(18,4) DEFAULT 0 COMMENT '当前库存',
  shortage      DECIMAL(18,4) DEFAULT 0 COMMENT '补充数量',
  stock_status  VARCHAR(32)   DEFAULT 'unknown' COMMENT 'sufficient/insufficient/occupied/out_of_stock',
  remark        VARCHAR(512)  DEFAULT NULL COMMENT '已出库时显示钉钉出库单号',
  create_by     VARCHAR(64)   DEFAULT NULL,
  create_time   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_plan_no (plan_no),
  KEY idx_material_id (material_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='备料计划单';

-- ============ 13. 角色 ============
CREATE TABLE IF NOT EXISTS sys_role (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  code        VARCHAR(32)  NOT NULL COMMENT '角色编码',
  name        VARCHAR(64)  NOT NULL COMMENT '角色名称',
  data_scope  VARCHAR(16)  NOT NULL DEFAULT 'all' COMMENT '数据范围 self/dept/all',
  description VARCHAR(255) DEFAULT NULL,
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色';

-- ============ 14. 权限 ============
CREATE TABLE IF NOT EXISTS sys_permission (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  code        VARCHAR(64)  NOT NULL COMMENT '权限编码，如 menu:inbound / btn:inbound:confirm',
  name        VARCHAR(64)  NOT NULL,
  type        VARCHAR(16)  NOT NULL DEFAULT 'menu' COMMENT 'menu/button',
  sort        INT          DEFAULT 0,
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='权限';

-- ============ 15. 角色-权限 ============
CREATE TABLE IF NOT EXISTS sys_role_permission (
  id            BIGINT NOT NULL AUTO_INCREMENT,
  role_id       BIGINT NOT NULL,
  permission_id BIGINT NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_role_perm (role_id, permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色-权限关联';

-- ============ 16. 用户-附加角色 ============
CREATE TABLE IF NOT EXISTS sys_user_role (
  id      BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_role (user_id, role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户-附加角色关联';

-- ============ 17. 系统配置 (备份策略/日志保留等) ============
CREATE TABLE IF NOT EXISTS sys_config (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  config_key    VARCHAR(64)  NOT NULL,
  config_value  VARCHAR(1024) DEFAULT NULL,
  description   VARCHAR(255) DEFAULT NULL,
  update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_config_key (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统配置';

-- ============ 18. 补货申请 ============
CREATE TABLE IF NOT EXISTS purchase_request (
  id            BIGINT        NOT NULL AUTO_INCREMENT,
  material_id   BIGINT        DEFAULT NULL,
  material_code VARCHAR(64)   DEFAULT NULL,
  material_name VARCHAR(128)  DEFAULT NULL,
  manufacturer  VARCHAR(128)  DEFAULT NULL COMMENT '厂家联系方式提示',
  quantity      DECIMAL(18,4) NOT NULL DEFAULT 0 COMMENT '采购数量',
  remark        VARCHAR(512)  DEFAULT NULL,
  status        VARCHAR(16)   NOT NULL DEFAULT 'pending' COMMENT 'pending/processed',
  create_by     VARCHAR(64)   DEFAULT NULL,
  create_time   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_material_id (material_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='补货申请';

-- ============ 19. CIS同步记录 ============
CREATE TABLE IF NOT EXISTS cis_sync_log (
  id             BIGINT       NOT NULL AUTO_INCREMENT,
  sync_type      VARCHAR(16)  NOT NULL COMMENT 'full/incremental',
  sync_status    VARCHAR(16)  NOT NULL COMMENT 'success/failed',
  material_count INT          DEFAULT 0,
  message        VARCHAR(1024) DEFAULT NULL,
  create_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='CIS同步日志';

-- =============================================================
-- 初始数据
-- =============================================================
-- 内置角色（权限按 2.6.2：工程师=登录+生产领料+物料检索/库存查询；库管员=全部）
INSERT IGNORE INTO sys_role (code, name, data_scope, description) VALUES
('admin',     '管理员', 'all',  '系统管理员，全部功能'),
('warehouse', '库管员', 'all',  '库房管理，全部业务功能'),
('engineer',  '工程师', 'self', '最低权限：登录、生产领料、物料检索/库存查询'),
('purchaser', '采购员', 'dept', '采购相关'),
('inspector', '质检员', 'dept', '质检相关'),
('manager',   '部门主管', 'dept', '部门主管');

INSERT IGNORE INTO sys_permission (code, name, type, sort) VALUES
('menu:inbound:purchase',  '采购入库',       'menu', 10),
('menu:inbound:return',    '退库入库',       'menu', 11),
('menu:inbound:records',   '入库记录',       'menu', 12),
('menu:outbound:picking',  '生产领料',       'menu', 20),
('menu:outbound:records',  '出库记录',       'menu', 21),
('menu:inventory:search',  '物料检索',       'menu', 30),
('menu:inventory:query',   '库存查询',       'menu', 31),
('menu:inventory:alert',   '库存预警',       'menu', 32),
('menu:inventory:flow',    '库存流水',       'menu', 33),
('menu:inventory:cis',     '同步CIS元件库',   'menu', 34),
('menu:report:inventory',  '库存明细',       'menu', 40),
('menu:report:inbound',    '入库统计',       'menu', 41),
('menu:report:outbound',   '出库统计',       'menu', 42),
('menu:report:stagnant',   '呆滞物品',       'menu', 43),
('menu:report:export',     '导出报表',       'menu', 44),
('menu:system:users',      '用户管理',       'menu', 50),
('menu:system:roles',      '角色权限',       'menu', 51),
('menu:system:backup',     '数据备份',       'menu', 52),
('menu:system:logs',       '系统日志',       'menu', 53),
('menu:system:password',   '密码修改',       'menu', 54),
('btn:inbound:confirm',    '确认入库',       'button', 60),
('btn:inbound:batchAudit', '批量审核入库',   'button', 61),
('btn:outbound:confirm',   '确认出库',       'button', 62),
('btn:outbound:reject',    '驳回出库',       'button', 63),
('btn:alert:handle',       '处理预警',       'button', 64),
('btn:alert:scan',         '手动扫描预警',   'button', 65),
('btn:backup:run',         '手动备份',       'button', 66),
('btn:cis:sync',           '同步CIS',        'button', 67);

-- 管理员/库管员拥有全部权限
INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM sys_role r, sys_permission p WHERE r.code IN ('admin', 'warehouse');

-- 工程师：生产领料、物料检索、库存查询、密码修改
INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM sys_role r, sys_permission p
WHERE r.code = 'engineer' AND p.code IN ('menu:outbound:picking','menu:inventory:search','menu:inventory:query','menu:system:password');

-- 采购员：入库记录、库存查询/预警/流水、报表查看、密码修改
INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM sys_role r, sys_permission p
WHERE r.code = 'purchaser' AND p.code IN ('menu:inbound:records','menu:inventory:search','menu:inventory:query','menu:inventory:alert','menu:inventory:flow','menu:report:inventory','menu:report:inbound','menu:report:stagnant','menu:system:password','btn:alert:handle');

-- 质检员：入库记录、库存查询、流水
INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM sys_role r, sys_permission p
WHERE r.code = 'inspector' AND p.code IN ('menu:inbound:records','menu:inventory:search','menu:inventory:query','menu:inventory:flow','menu:system:password');

-- 部门主管：业务+报表
INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM sys_role r, sys_permission p
WHERE r.code = 'manager' AND p.code IN ('menu:inbound:purchase','menu:inbound:return','menu:inbound:records','menu:outbound:picking','menu:outbound:records','menu:inventory:search','menu:inventory:query','menu:inventory:alert','menu:inventory:flow','menu:report:inventory','menu:report:inbound','menu:report:outbound','menu:report:stagnant','menu:report:export','menu:system:password');

-- 系统配置：备份策略（每周日02:00全量、每日增量、保留30天）、日志保留（1年）
INSERT IGNORE INTO sys_config (config_key, config_value, description) VALUES
('backup.full.cron',   '0 0 2 * * 0',    '全量备份cron（每周日凌晨2点）'),
('backup.incr.cron',   '0 0 2 * * ?',    '增量备份cron（每日凌晨2点）'),
('backup.retention',   '30',             '备份保留天数'),
('backup.dir',         'bms_backup',     '备份目录（相对用户主目录）'),
('log.retention.days', '365',            '系统日志保留天数');

-- 默认管理员账号 admin / Abc@12345（首次部署后请立即修改）
INSERT IGNORE INTO sys_user (username, password, real_name, phone, dept, role, status)
VALUES ('admin', '$2b$10$QLxeH.e8TByQ9EjFCUDC9OKAYAJ9d4UxwLLAiw3/SmK8ey5VEC3P2',
        '系统管理员', '13800000000', '系统部', 'admin', 1);
