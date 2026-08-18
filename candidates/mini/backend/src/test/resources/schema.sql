-- =====================================================================
-- Material Management System (BMS) - MySQL schema
-- This schema is idempotent and can be executed on any MySQL 8.x server.
-- It is also compatible with H2 in MySQL mode for automated tests.
-- =====================================================================

CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    password VARCHAR(100) NOT NULL,
    real_name VARCHAR(50),
    phone VARCHAR(20),
    dept VARCHAR(50),
    role VARCHAR(20) DEFAULT 'engineer',
    status INT DEFAULT 1,
    dingtalk_union_id VARCHAR(100),
    create_time TIMESTAMP NULL,
    update_time TIMESTAMP NULL,
    CONSTRAINT uk_sys_user_username UNIQUE (username)
);

CREATE TABLE IF NOT EXISTS sys_login_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50),
    login_ip VARCHAR(50),
    device_info VARCHAR(255),
    login_time TIMESTAMP NULL,
    login_result INT
);

CREATE TABLE IF NOT EXISTS sys_operation_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50),
    operation VARCHAR(50),
    description VARCHAR(500),
    ip VARCHAR(50),
    result VARCHAR(20),
    create_time TIMESTAMP NULL
);

CREATE TABLE IF NOT EXISTS tb_material (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    material_code VARCHAR(50),
    material_name VARCHAR(100),
    package_type VARCHAR(50),
    value_data VARCHAR(100),
    spec_model VARCHAR(100),
    warehouse_code VARCHAR(50),
    location_no VARCHAR(50),
    remark VARCHAR(500),
    stock DECIMAL(18,2) DEFAULT 0,
    version INT DEFAULT 0,
    lock_stock DECIMAL(18,2) DEFAULT 0,
    min_stock DECIMAL(18,2),
    max_stock DECIMAL(18,2),
    stagnation_days INT,
    material_cost DECIMAL(18,4),
    create_time TIMESTAMP NULL,
    update_time TIMESTAMP NULL
);

CREATE TABLE IF NOT EXISTS tb_inbound_order (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    bill_no VARCHAR(50),
    supplier VARCHAR(100),
    apply_user VARCHAR(50),
    apply_time TIMESTAMP NULL,
    in_date TIMESTAMP NULL,
    remark VARCHAR(500),
    ding_instance_id VARCHAR(100),
    order_status INT DEFAULT 0,
    create_time TIMESTAMP NULL,
    update_time TIMESTAMP NULL
);

CREATE TABLE IF NOT EXISTS in_storage_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    inbound_id BIGINT,
    material_id BIGINT,
    material_code VARCHAR(50),
    num DECIMAL(18,2),
    batch_no VARCHAR(50),
    create_time TIMESTAMP NULL
);

CREATE TABLE IF NOT EXISTS in_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    bill_no VARCHAR(50),
    material_id BIGINT,
    batch_no VARCHAR(50),
    in_num DECIMAL(18,2),
    in_user VARCHAR(50),
    in_time TIMESTAMP NULL,
    create_time TIMESTAMP NULL
);

CREATE TABLE IF NOT EXISTS tb_outbound_order (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    outbound_code VARCHAR(50),
    out_type INT,
    apply_user VARCHAR(50),
    oper_user VARCHAR(50),
    remark VARCHAR(500),
    order_status INT DEFAULT 0,
    ding_instance_id VARCHAR(100),
    create_time TIMESTAMP NULL,
    update_time TIMESTAMP NULL
);

CREATE TABLE IF NOT EXISTS tb_out_storage_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    outbound_id BIGINT,
    material_id BIGINT,
    material_code VARCHAR(50),
    batch_no VARCHAR(50),
    out_num DECIMAL(18,2),
    create_time TIMESTAMP NULL
);

CREATE TABLE IF NOT EXISTS tb_out_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    outbound_code VARCHAR(50),
    material_id BIGINT,
    batch_no VARCHAR(50),
    out_num DECIMAL(18,2),
    out_user VARCHAR(50),
    out_time TIMESTAMP NULL
);

CREATE TABLE IF NOT EXISTS sys_stock_alert (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    material_id BIGINT,
    material_code VARCHAR(50),
    material_name VARCHAR(100),
    alert_type INT,
    current_stock DECIMAL(18,2),
    threshold_stock DECIMAL(18,2),
    handled INT DEFAULT 0,
    handler VARCHAR(50),
    handle_method VARCHAR(100),
    handle_time TIMESTAMP NULL,
    create_time TIMESTAMP NULL
);

-- =====================================================================
-- New tables for BOM, roles, backup/log retention, CIS sync, restock
-- =====================================================================

CREATE TABLE IF NOT EXISTS tb_bom (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    bom_code VARCHAR(50),
    bom_name VARCHAR(100),
    version VARCHAR(20),
    status INT DEFAULT 1,
    creator VARCHAR(50),
    create_time TIMESTAMP NULL,
    update_time TIMESTAMP NULL
);

CREATE TABLE IF NOT EXISTS tb_bom_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    bom_id BIGINT,
    material_id BIGINT,
    material_code VARCHAR(50),
    material_name VARCHAR(100),
    package_type VARCHAR(50),
    value_data VARCHAR(100),
    spec_model VARCHAR(100),
    batch_no VARCHAR(50),
    need_num DECIMAL(18,2),
    remark VARCHAR(500)
);

CREATE TABLE IF NOT EXISTS tb_bom_match_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    bom_id BIGINT,
    bom_version VARCHAR(20),
    material_code VARCHAR(50),
    material_name VARCHAR(100),
    stock_status VARCHAR(20),
    current_stock DECIMAL(18,2),
    need_num DECIMAL(18,2),
    shortage DECIMAL(18,2),
    outbound_code VARCHAR(50),
    remark VARCHAR(500),
    match_time TIMESTAMP NULL
);

CREATE TABLE IF NOT EXISTS tb_pick_plan (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    plan_no VARCHAR(50),
    bom_id BIGINT,
    bom_version VARCHAR(20),
    creator VARCHAR(50),
    remark VARCHAR(500),
    create_time TIMESTAMP NULL
);

CREATE TABLE IF NOT EXISTS tb_pick_plan_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    plan_id BIGINT,
    material_code VARCHAR(50),
    material_name VARCHAR(100),
    package_type VARCHAR(50),
    value_data VARCHAR(100),
    spec_model VARCHAR(100),
    stock DECIMAL(18,2),
    need_num DECIMAL(18,2),
    supplement_num DECIMAL(18,2),
    remark VARCHAR(500)
);

CREATE TABLE IF NOT EXISTS sys_role (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_code VARCHAR(50) NOT NULL,
    role_name VARCHAR(50) NOT NULL,
    description VARCHAR(200),
    create_time TIMESTAMP NULL,
    update_time TIMESTAMP NULL
);

CREATE TABLE IF NOT EXISTS sys_role_menu (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_code VARCHAR(50) NOT NULL,
    menu_path VARCHAR(200) NOT NULL,
    button_code VARCHAR(100),
    data_scope VARCHAR(20) DEFAULT 'all'
);

CREATE TABLE IF NOT EXISTS sys_restock_request (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    material_id BIGINT,
    material_code VARCHAR(50),
    material_name VARCHAR(100),
    supplier_contact VARCHAR(100),
    purchase_qty DECIMAL(18,2),
    requester VARCHAR(50),
    status INT DEFAULT 0,
    create_time TIMESTAMP NULL
);

CREATE TABLE IF NOT EXISTS sys_cis_sync_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sync_type VARCHAR(20),
    sync_mode VARCHAR(20),
    total_count INT,
    success_count INT,
    fail_count INT,
    status VARCHAR(20),
    error_msg VARCHAR(500),
    create_time TIMESTAMP NULL
);

CREATE TABLE IF NOT EXISTS sys_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    config_key VARCHAR(100) NOT NULL,
    config_value VARCHAR(500),
    description VARCHAR(200),
    update_time TIMESTAMP NULL,
    CONSTRAINT uk_sys_config_key UNIQUE (config_key)
);

CREATE TABLE IF NOT EXISTS sys_backup_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    backup_type VARCHAR(20),
    file_path VARCHAR(500),
    file_size BIGINT,
    status VARCHAR(20),
    create_time TIMESTAMP NULL
);
