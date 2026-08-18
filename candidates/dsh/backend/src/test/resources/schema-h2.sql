-- =============================================================
-- H2 测试库 schema（MySQL兼容模式），仅用于自动化测试
-- 与 src/main/resources/schema.sql 保持一致
-- =============================================================

-- H2 兼容 MySQL 函数别名（DATE_FORMAT / DATEDIFF）
CREATE ALIAS IF NOT EXISTS DATE_FORMAT FOR "com.koolearn.bms.test.H2DateFunctions.dateFormat";

CREATE TABLE IF NOT EXISTS sys_user (
  id                BIGINT       NOT NULL AUTO_INCREMENT,
  username          VARCHAR(64)  NOT NULL,
  password          VARCHAR(100) NOT NULL,
  real_name         VARCHAR(64)  DEFAULT NULL,
  phone             VARCHAR(20)  DEFAULT NULL,
  dept              VARCHAR(64)  DEFAULT NULL,
  role              VARCHAR(32)  NOT NULL DEFAULT 'engineer',
  status            TINYINT      NOT NULL DEFAULT 1,
  dingtalk_union_id VARCHAR(128) DEFAULT NULL,
  last_login_time   DATETIME     DEFAULT NULL,
  create_time       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  CONSTRAINT uk_sys_user_username UNIQUE (username),
  CONSTRAINT uk_sys_user_phone UNIQUE (phone),
  CONSTRAINT uk_sys_user_ding UNIQUE (dingtalk_union_id)
);

CREATE TABLE IF NOT EXISTS tb_material (
  id              BIGINT        NOT NULL AUTO_INCREMENT,
  material_code   VARCHAR(64)   DEFAULT NULL,
  material_name   VARCHAR(128)  NOT NULL,
  package_type    VARCHAR(64)   DEFAULT NULL,
  value_data      VARCHAR(64)   DEFAULT NULL,
  spec_model      VARCHAR(128)  DEFAULT NULL,
  manufacturer    VARCHAR(128)  DEFAULT NULL,
  warehouse_code  VARCHAR(64)   DEFAULT NULL,
  location_no     VARCHAR(64)   DEFAULT NULL,
  remark          VARCHAR(512)  DEFAULT NULL,
  stock           DECIMAL(18,4) NOT NULL DEFAULT 0,
  lock_stock      DECIMAL(18,4) NOT NULL DEFAULT 0,
  min_stock       DECIMAL(18,4) DEFAULT NULL,
  max_stock       DECIMAL(18,4) DEFAULT NULL,
  stagnation_days INT           DEFAULT 90,
  material_cost   DECIMAL(12,4) DEFAULT 0,
  version         INT           NOT NULL DEFAULT 0,
  create_time     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  CONSTRAINT uk_tb_material_code UNIQUE (material_code)
);

CREATE TABLE IF NOT EXISTS tb_inbound_order (
  id               BIGINT       NOT NULL AUTO_INCREMENT,
  bill_no          VARCHAR(64)  NOT NULL,
  in_type          TINYINT      NOT NULL DEFAULT 1,
  return_reason    VARCHAR(255) DEFAULT NULL,
  supplier         VARCHAR(128) DEFAULT NULL,
  apply_user       VARCHAR(64)  DEFAULT NULL,
  apply_time       DATETIME     DEFAULT NULL,
  in_date          DATETIME     DEFAULT NULL,
  remark           VARCHAR(512) DEFAULT NULL,
  ding_instance_id VARCHAR(128) DEFAULT NULL,
  order_status     TINYINT      NOT NULL DEFAULT 0,
  create_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  CONSTRAINT uk_inbound_ding UNIQUE (ding_instance_id)
);

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
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS tb_outbound_order (
  id               BIGINT       NOT NULL AUTO_INCREMENT,
  outbound_code    VARCHAR(64)  NOT NULL,
  out_type         TINYINT      NOT NULL DEFAULT 1,
  apply_user       VARCHAR(64)  DEFAULT NULL,
  oper_user        VARCHAR(64)  DEFAULT NULL,
  remark           VARCHAR(512) DEFAULT NULL,
  order_status     TINYINT      NOT NULL DEFAULT 0,
  ding_instance_id VARCHAR(128) DEFAULT NULL,
  create_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  CONSTRAINT uk_outbound_ding UNIQUE (ding_instance_id)
);

CREATE TABLE IF NOT EXISTS tb_out_storage_item (
  id            BIGINT        NOT NULL AUTO_INCREMENT,
  outbound_id   BIGINT        NOT NULL,
  material_id   BIGINT        DEFAULT NULL,
  material_code VARCHAR(64)   DEFAULT NULL,
  material_name VARCHAR(128)  DEFAULT NULL,
  batch_no      VARCHAR(64)   DEFAULT NULL,
  out_num       DECIMAL(18,4) NOT NULL,
  create_time   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS in_record (
  id            BIGINT        NOT NULL AUTO_INCREMENT,
  bill_no       VARCHAR(64)   DEFAULT NULL,
  material_id   BIGINT        DEFAULT NULL,
  material_code VARCHAR(64)   DEFAULT NULL,
  material_name VARCHAR(128)  DEFAULT NULL,
  batch_no      VARCHAR(64)   DEFAULT NULL,
  location_no   VARCHAR(64)   DEFAULT NULL,
  supplier      VARCHAR(128)  DEFAULT NULL,
  in_num        DECIMAL(18,4) NOT NULL,
  in_user       VARCHAR(64)   DEFAULT NULL,
  in_time       DATETIME      NOT NULL,
  create_time   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS tb_out_record (
  id            BIGINT        NOT NULL AUTO_INCREMENT,
  outbound_code VARCHAR(64)   DEFAULT NULL,
  material_id   BIGINT        DEFAULT NULL,
  material_code VARCHAR(64)   DEFAULT NULL,
  material_name VARCHAR(128)  DEFAULT NULL,
  batch_no      VARCHAR(64)   DEFAULT NULL,
  dept          VARCHAR(64)   DEFAULT NULL,
  out_num       DECIMAL(18,4) NOT NULL,
  out_user      VARCHAR(64)   DEFAULT NULL,
  out_time      DATETIME      NOT NULL,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS sys_stock_alert (
  id              BIGINT        NOT NULL AUTO_INCREMENT,
  material_id     BIGINT        DEFAULT NULL,
  material_code   VARCHAR(64)   DEFAULT NULL,
  material_name   VARCHAR(128)  DEFAULT NULL,
  alert_type      TINYINT       NOT NULL,
  current_stock   DECIMAL(18,4) DEFAULT NULL,
  threshold_stock DECIMAL(18,4) DEFAULT NULL,
  handled         TINYINT       NOT NULL DEFAULT 0,
  handler         VARCHAR(64)   DEFAULT NULL,
  handle_method   VARCHAR(128)  DEFAULT NULL,
  handle_time     DATETIME      DEFAULT NULL,
  notify_result   VARCHAR(255)  DEFAULT NULL,
  create_time     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS sys_login_log (
  id           BIGINT       NOT NULL AUTO_INCREMENT,
  username     VARCHAR(64)  DEFAULT NULL,
  login_ip     VARCHAR(64)  DEFAULT NULL,
  device_info  VARCHAR(512) DEFAULT NULL,
  login_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  login_result TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS sys_operation_log (
  id          BIGINT        NOT NULL AUTO_INCREMENT,
  username    VARCHAR(64)   DEFAULT NULL,
  operation   VARCHAR(128)  DEFAULT NULL,
  description VARCHAR(1024) DEFAULT NULL,
  ip          VARCHAR(64)   DEFAULT NULL,
  result      VARCHAR(64)   DEFAULT '成功',
  create_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS bom_plan (
  id            BIGINT        NOT NULL AUTO_INCREMENT,
  plan_no       VARCHAR(64)   NOT NULL,
  bom_version   VARCHAR(64)   DEFAULT NULL,
  material_id   BIGINT        DEFAULT NULL,
  material_code VARCHAR(64)   DEFAULT NULL,
  material_name VARCHAR(128)  DEFAULT NULL,
  package_type  VARCHAR(64)   DEFAULT NULL,
  value_data    VARCHAR(64)   DEFAULT NULL,
  spec_model    VARCHAR(128)  DEFAULT NULL,
  batch_no      VARCHAR(64)   DEFAULT NULL,
  need_num      DECIMAL(18,4) NOT NULL,
  current_stock DECIMAL(18,4) DEFAULT 0,
  shortage      DECIMAL(18,4) DEFAULT 0,
  stock_status  VARCHAR(32)   DEFAULT 'unknown',
  remark        VARCHAR(512)  DEFAULT NULL,
  create_by     VARCHAR(64)   DEFAULT NULL,
  create_time   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS sys_role (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  code        VARCHAR(32)  NOT NULL,
  name        VARCHAR(64)  NOT NULL,
  data_scope  VARCHAR(16)  NOT NULL DEFAULT 'all',
  description VARCHAR(255) DEFAULT NULL,
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  CONSTRAINT uk_role_code UNIQUE (code)
);

CREATE TABLE IF NOT EXISTS sys_permission (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  code        VARCHAR(64)  NOT NULL,
  name        VARCHAR(64)  NOT NULL,
  type        VARCHAR(16)  NOT NULL DEFAULT 'menu',
  sort        INT          DEFAULT 0,
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  CONSTRAINT uk_perm_code UNIQUE (code)
);

CREATE TABLE IF NOT EXISTS sys_role_permission (
  id            BIGINT NOT NULL AUTO_INCREMENT,
  role_id       BIGINT NOT NULL,
  permission_id BIGINT NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT uk_role_perm UNIQUE (role_id, permission_id)
);

CREATE TABLE IF NOT EXISTS sys_user_role (
  id      BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT uk_user_role UNIQUE (user_id, role_id)
);

CREATE TABLE IF NOT EXISTS sys_config (
  id           BIGINT        NOT NULL AUTO_INCREMENT,
  config_key   VARCHAR(64)   NOT NULL,
  config_value VARCHAR(1024) DEFAULT NULL,
  description  VARCHAR(255)  DEFAULT NULL,
  update_time  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  CONSTRAINT uk_config_key UNIQUE (config_key)
);

CREATE TABLE IF NOT EXISTS purchase_request (
  id            BIGINT        NOT NULL AUTO_INCREMENT,
  material_id   BIGINT        DEFAULT NULL,
  material_code VARCHAR(64)   DEFAULT NULL,
  material_name VARCHAR(128)  DEFAULT NULL,
  manufacturer  VARCHAR(128)  DEFAULT NULL,
  quantity      DECIMAL(18,4) NOT NULL DEFAULT 0,
  remark        VARCHAR(512)  DEFAULT NULL,
  status        VARCHAR(16)   NOT NULL DEFAULT 'pending',
  create_by     VARCHAR(64)   DEFAULT NULL,
  create_time   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS cis_sync_log (
  id             BIGINT        NOT NULL AUTO_INCREMENT,
  sync_type      VARCHAR(16)   NOT NULL,
  sync_status    VARCHAR(16)   NOT NULL,
  material_count INT           DEFAULT 0,
  message        VARCHAR(1024) DEFAULT NULL,
  create_time    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);

-- ============ 测试初始数据 ============
-- admin / Abc@12345
INSERT INTO sys_user (username, password, real_name, phone, dept, role, status)
VALUES ('admin', '$2b$10$QLxeH.e8TByQ9EjFCUDC9OKAYAJ9d4UxwLLAiw3/SmK8ey5VEC3P2', '系统管理员', '13800000000', '系统部', 'admin', 1);
-- warehouse / Abc@12345
INSERT INTO sys_user (username, password, real_name, phone, dept, role, status)
VALUES ('wh01', '$2b$10$QLxeH.e8TByQ9EjFCUDC9OKAYAJ9d4UxwLLAiw3/SmK8ey5VEC3P2', '库管员', '13800000001', '仓库部', 'warehouse', 1);
-- engineer / Abc@12345
INSERT INTO sys_user (username, password, real_name, phone, dept, role, status)
VALUES ('eng01', '$2b$10$QLxeH.e8TByQ9EjFCUDC9OKAYAJ9d4UxwLLAiw3/SmK8ey5VEC3P2', '工程师', '13800000002', '硬件部', 'engineer', 1);

INSERT INTO sys_role (code, name, data_scope, description) VALUES
('admin', '管理员', 'all', ''),
('warehouse', '库管员', 'all', ''),
('engineer', '工程师', 'self', ''),
('purchaser', '采购员', 'dept', ''),
('inspector', '质检员', 'dept', ''),
('manager', '部门主管', 'dept', '');

INSERT INTO sys_permission (code, name, type, sort) VALUES
('menu:inbound:purchase', '采购入库', 'menu', 10),
('menu:inbound:return', '退库入库', 'menu', 11),
('menu:inbound:records', '入库记录', 'menu', 12),
('menu:outbound:picking', '生产领料', 'menu', 20),
('menu:outbound:records', '出库记录', 'menu', 21),
('menu:inventory:search', '物料检索', 'menu', 30),
('menu:inventory:query', '库存查询', 'menu', 31),
('menu:inventory:alert', '库存预警', 'menu', 32),
('menu:inventory:flow', '库存流水', 'menu', 33),
('menu:inventory:cis', '同步CIS元件库', 'menu', 34),
('menu:report:inventory', '库存明细', 'menu', 40),
('menu:report:inbound', '入库统计', 'menu', 41),
('menu:report:outbound', '出库统计', 'menu', 42),
('menu:report:stagnant', '呆滞物品', 'menu', 43),
('menu:report:export', '导出报表', 'menu', 44),
('menu:system:users', '用户管理', 'menu', 50),
('menu:system:roles', '角色权限', 'menu', 51),
('menu:system:backup', '数据备份', 'menu', 52),
('menu:system:logs', '系统日志', 'menu', 53),
('menu:system:password', '密码修改', 'menu', 54),
('btn:inbound:confirm', '确认入库', 'button', 60),
('btn:inbound:batchAudit', '批量审核入库', 'button', 61),
('btn:outbound:confirm', '确认出库', 'button', 62),
('btn:outbound:reject', '驳回出库', 'button', 63),
('btn:alert:handle', '处理预警', 'button', 64),
('btn:alert:scan', '手动扫描预警', 'button', 65),
('btn:backup:run', '手动备份', 'button', 66),
('btn:cis:sync', '同步CIS', 'button', 67);

INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM sys_role r, sys_permission p WHERE r.code IN ('admin', 'warehouse');

INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM sys_role r, sys_permission p
WHERE r.code = 'engineer' AND p.code IN ('menu:outbound:picking','menu:inventory:search','menu:inventory:query','menu:system:password');

INSERT INTO sys_config (config_key, config_value, description) VALUES
('backup.full.cron', '0 0 2 * * 0', ''),
('backup.incr.cron', '0 0 2 * * ?', ''),
('backup.retention', '30', ''),
('backup.dir', 'bms_backup', ''),
('log.retention.days', '365', '');
