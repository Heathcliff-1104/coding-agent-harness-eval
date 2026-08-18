-- =====================================================================
-- 物料管理系统数据库表结构
-- 兼容 MySQL 与 H2 (MODE=MySQL, DATABASE_TO_LOWER=TRUE)
-- 全部使用幂等 CREATE TABLE IF NOT EXISTS，可重复执行
-- =====================================================================

-- 用户表
CREATE TABLE IF NOT EXISTS sys_user (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(64) NOT NULL UNIQUE,
  password VARCHAR(128),
  real_name VARCHAR(64),
  phone VARCHAR(20) UNIQUE,
  dept VARCHAR(64),
  role VARCHAR(32) DEFAULT 'engineer',
  status INT DEFAULT 1,
  dingtalk_union_id VARCHAR(128),
  create_time DATETIME,
  update_time DATETIME
);

-- 物料表
CREATE TABLE IF NOT EXISTS tb_material (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  material_code VARCHAR(64),
  material_name VARCHAR(128),
  package_type VARCHAR(64),
  value_data VARCHAR(128),
  spec_model VARCHAR(128),
  warehouse_code VARCHAR(64),
  location_no VARCHAR(64),
  remark VARCHAR(255),
  stock DECIMAL(18,2) DEFAULT 0,
  lock_stock DECIMAL(18,2) DEFAULT 0,
  min_stock DECIMAL(18,2),
  max_stock DECIMAL(18,2),
  stagnation_days INT DEFAULT 90,
  material_cost DECIMAL(18,2),
  manufacturer_name VARCHAR(128),
  manufacturer_batch VARCHAR(128),
  version INT DEFAULT 0,
  create_time DATETIME,
  update_time DATETIME
);

-- 入库单表
CREATE TABLE IF NOT EXISTS tb_inbound_order (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  bill_no VARCHAR(64),
  supplier VARCHAR(128),
  apply_user VARCHAR(64),
  apply_time DATETIME,
  in_date DATETIME,
  in_type VARCHAR(20),
  return_reason VARCHAR(255),
  remark VARCHAR(255),
  ding_instance_id VARCHAR(128),
  order_status INT DEFAULT 0,
  create_time DATETIME,
  update_time DATETIME
);

-- 入库单明细
CREATE TABLE IF NOT EXISTS in_storage_item (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  inbound_id BIGINT,
  material_id BIGINT,
  material_code VARCHAR(64),
  num DECIMAL(18,2),
  batch_no VARCHAR(64),
  location_no VARCHAR(64),
  create_time DATETIME
);

-- 入库记录
CREATE TABLE IF NOT EXISTS in_record (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  bill_no VARCHAR(64),
  material_id BIGINT,
  material_code VARCHAR(64),
  material_name VARCHAR(128),
  batch_no VARCHAR(64),
  in_num DECIMAL(18,2),
  in_user VARCHAR(64),
  in_time DATETIME,
  location_no VARCHAR(64),
  create_time DATETIME
);

-- 出库单表
CREATE TABLE IF NOT EXISTS tb_outbound_order (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  outbound_code VARCHAR(64),
  out_type INT,
  apply_user VARCHAR(64),
  oper_user VARCHAR(64),
  remark VARCHAR(255),
  order_status INT DEFAULT 0,
  ding_instance_id VARCHAR(128),
  create_time DATETIME,
  update_time DATETIME
);

-- 出库单明细
CREATE TABLE IF NOT EXISTS tb_out_storage_item (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  outbound_id BIGINT,
  material_id BIGINT,
  material_code VARCHAR(64),
  batch_no VARCHAR(64),
  out_num DECIMAL(18,2),
  create_time DATETIME
);

-- 出库记录
CREATE TABLE IF NOT EXISTS tb_out_record (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  outbound_code VARCHAR(64),
  material_id BIGINT,
  material_code VARCHAR(64),
  material_name VARCHAR(128),
  batch_no VARCHAR(64),
  out_num DECIMAL(18,2),
  out_user VARCHAR(64),
  out_time DATETIME,
  create_time DATETIME
);

-- 库存预警
CREATE TABLE IF NOT EXISTS sys_stock_alert (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  material_id BIGINT,
  material_code VARCHAR(64),
  material_name VARCHAR(128),
  alert_type INT,
  current_stock DECIMAL(18,2),
  threshold_stock DECIMAL(18,2),
  handled INT DEFAULT 0,
  handler VARCHAR(64),
  handle_method VARCHAR(64),
  handle_time DATETIME,
  create_time DATETIME
);

-- 登录日志
CREATE TABLE IF NOT EXISTS sys_login_log (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(64),
  login_ip VARCHAR(64),
  device_info VARCHAR(255),
  login_time DATETIME,
  login_result INT
);

-- 操作日志
CREATE TABLE IF NOT EXISTS sys_operation_log (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(64),
  operation VARCHAR(64),
  description VARCHAR(255),
  ip VARCHAR(64),
  result VARCHAR(64),
  create_time DATETIME
);

-- 角色表
CREATE TABLE IF NOT EXISTS sys_role (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  role_code VARCHAR(32),
  role_name VARCHAR(64),
  data_scope VARCHAR(16) DEFAULT 'all',
  description VARCHAR(255),
  create_time DATETIME,
  update_time DATETIME
);

-- 权限表（菜单/按钮）
CREATE TABLE IF NOT EXISTS sys_permission (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  perm_code VARCHAR(64),
  perm_name VARCHAR(64),
  perm_type VARCHAR(16),
  parent_code VARCHAR(64),
  path VARCHAR(128),
  sort_no INT DEFAULT 0,
  create_time DATETIME
);

-- 角色-权限关联表
CREATE TABLE IF NOT EXISTS sys_role_permission (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  role_id BIGINT,
  permission_id BIGINT
);

-- BOM 表头
CREATE TABLE IF NOT EXISTS bom_header (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  bom_no VARCHAR(64),
  bom_name VARCHAR(128),
  version INT DEFAULT 1,
  repeat_flag INT DEFAULT 0,
  create_user VARCHAR(64),
  create_time DATETIME
);

-- BOM 明细
CREATE TABLE IF NOT EXISTS bom_item (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  bom_id BIGINT,
  material_code VARCHAR(64),
  material_name VARCHAR(128),
  package_type VARCHAR(64),
  spec_model VARCHAR(128),
  batch_no VARCHAR(64),
  need_num DECIMAL(18,2),
  remark VARCHAR(255)
);

-- CIS 同步日志
CREATE TABLE IF NOT EXISTS cis_sync_log (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  sync_type VARCHAR(16),
  status VARCHAR(16),
  rows INT DEFAULT 0,
  message VARCHAR(255),
  sync_time DATETIME
);

-- 补货申请
CREATE TABLE IF NOT EXISTS sys_replenishment (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  material_id BIGINT,
  material_code VARCHAR(64),
  material_name VARCHAR(128),
  shortage DECIMAL(18,2),
  supplier_contact VARCHAR(128),
  purchase_num DECIMAL(18,2),
  applicant VARCHAR(64),
  status INT DEFAULT 0,
  remark VARCHAR(255),
  create_time DATETIME
);

-- 备份配置
CREATE TABLE IF NOT EXISTS backup_config (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  backup_type VARCHAR(16),
  cron_expr VARCHAR(64),
  retention_days INT DEFAULT 30,
  enabled INT DEFAULT 1,
  last_run DATETIME,
  create_time DATETIME,
  update_time DATETIME
);

-- 备份记录
CREATE TABLE IF NOT EXISTS backup_record (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  backup_type VARCHAR(16),
  file_path VARCHAR(255),
  file_size BIGINT,
  status VARCHAR(16),
  message VARCHAR(255),
  create_time DATETIME
);
