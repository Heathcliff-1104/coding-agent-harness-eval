-- 初始化默认管理员（密码：admin123，首次登录后请修改）
INSERT INTO sys_user (username, password, real_name, phone, dept, role, status, create_time, update_time)
SELECT 'admin', '$2a$10$fhwH8SLDUsYQ7F3bqg7go.2CySrq8eJVCa6szy6bf/fjFrKT.zjde', '系统管理员', '13800000000', '系统部', 'admin', 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE username = 'admin');
