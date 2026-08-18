# 通用物料管理系统 (Material Management System)

物料采购入库、生产领料、退库、库存查询、库存预警、CIS 同步、报表统计与系统管理。

## 技术栈

- 后端：Spring Boot 2.7 / MyBatis-Plus / MySQL 8（测试使用 H2 MySQL 模式）
- 前端：Vue 3 + Vite + Element Plus + ECharts

## 目录

```
backend/   Spring Boot 后端
frontend/  Vue 3 前端
requirements/requirements.md  功能需求文档
schema.sql  数据库初始化脚本（backend/src/main/resources/schema.sql）
AUDIT.md   审计报告（初始 backlog）
IMPLEMENTATION_STATUS.md  实施状态
```

## 快速开始

### 1. 数据库

使用 `backend/src/main/resources/schema.sql` 初始化 MySQL（幂等 `CREATE TABLE IF NOT EXISTS`）：

```bash
mysql -u root -p < backend/src/main/resources/schema.sql
```

或直接启动后端，配置 `spring.sql.init.mode=always` 会自动执行 schema。

### 2. 后端

```bash
cd backend
mvn spring-boot:run
```

环境变量（可选）：

| 变量 | 说明 | 默认 |
|------|------|------|
| `DB_URL` | JDBC 连接 | `jdbc:mysql://localhost:3306/bms` |
| `DB_USERNAME` | 数据库用户 | `root` |
| `DB_PASSWORD` | 数据库密码 | 空 |
| `JWT_SECRET` | JWT 密钥（>=32字节），生产必须设置 | 本地默认值 |
| `BMS_JWT_STRICT` | 置 `true` 时默认密钥启动失败 | `false` |
| `CORS_ALLOWED_ORIGINS` | 允许的前端来源（逗号分隔） | `http://localhost:5173,http://127.0.0.1:5173` |
| `DINGTALK_CALLBACK_SECRET` | 钉钉回调签名密钥 | 空（演示模式） |
| `CIS_ADAPTER_MODE` | CIS 同步模式 `mock`/`http` | `mock` |
| `DINGTALK_ROBOT_WEBHOOK` | 钉钉机器人 webhook | 空（演示模式） |

### 3. 前端

```bash
cd frontend
npm install
npm run dev
```

### 4. 测试

```bash
cd backend
mvn test
```

### 5. 生产构建

```bash
cd backend && mvn package -DskipTests
cd frontend && npm run build
```

## 关键说明

- 注册用户固定分配 `engineer` 角色（最低权限），防止客户端提权。
- 后端每次请求都从数据库实时读取用户角色与状态，权限变更立即生效、禁用用户立即失效。
- 钉钉审批/CIS/机器人等在未配置外部服务时进入**演示模式**（mock），仅记录日志并返回确定性的本地结果。
- 数据库 schema 通过 `schema.sql` 幂等初始化，测试使用 H2 内存库。

## 未完成/受限项

详见 `IMPLEMENTATION_STATUS.md`。
