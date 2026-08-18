# 物料管理系统 (BMS) 后端

Spring Boot 2.7 (Java 8) + MyBatis-Plus + MySQL 的物料管理系统后端。

## 运行要求

- JDK 17（pom 目标为 Java 8 语法，JDK 17 可编译运行）
- Maven 3.9+
- MySQL 8（开发环境）；测试环境使用 H2 内存库（`test` profile）

## 快速开始

```bash
# 1. 准备数据库（schema.sql 会在启动时自动执行，无需手工建表）
mysql -uroot -p -e "CREATE DATABASE IF NOT EXISTS bms DEFAULT CHARACTER SET utf8mb4;"

# 2. 编译
mvn -q -DskipTests compile

# 3. 运行测试（使用 H2 内存库，无需 MySQL）
mvn -q test

# 4. 启动（默认 8080 端口）
mvn spring-boot:run
# 或打包后运行
mvn package -DskipTests
java -jar target/bms-1.0.0.jar
```

## 环境变量

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `DB_URL` | `jdbc:mysql://localhost:3306/bms` | 数据库连接串 |
| `DB_USERNAME` | `root` | 数据库用户 |
| `DB_PASSWORD` | 空 | 数据库密码 |
| `JWT_SECRET` | 内置默认值 | JWT 签名密钥（生产务必覆盖） |
| `DINGTALK_APP_KEY` / `DINGTALK_APP_SECRET` | demo-* | 钉钉应用凭证 |
| `DINGTALK_CALLBACK_TOKEN` | `demo-callback-token` | 钉钉审批回调验签 Token |
| `DINGTALK_MODE` | `mock` | 钉钉消息模式（`mock`=仅记日志 / `robot`=推机器人） |
| `DINGTALK_WEBHOOK_URL` | 空 | 钉钉机器人 webhook |
| `DEFAULT_PASSWORD` | `Sys@123456` | 管理员重置用户密码时的默认密码（满足密码策略：8-20位、≥3类字符） |
| `SYS_LOG_RETENTION_DAYS` | `365` | 系统日志保留天数 |
| `CIS_SYNC_MODE` | `mock` | CIS 同步模式（`mock`=仅记日志） |
| `CIS_SYNC_URL` | `http://localhost:9090/cis` | CIS 系统地址 |

## 种子账号

应用启动时由 `DataInitializer` 幂等写入：

- 管理员：`admin` / `Admin@123456`
- 库管员：`warehouse` / `Warehouse@123456`（仅当用户表为空时创建）

角色权限种子：admin（全部权限）、warehouse（除系统管理菜单外全部）、engineer（生产领料/物料检索/库存查询/密码修改）。

## 测试

测试使用 `application-test.properties` 中的 H2 内存库（`MODE=MySQL`），无需真实 MySQL：

```bash
mvn -q test
```

关键回归测试位于 `src/test/java/com/koolearn/bms/service/`。

## 目录结构

- `controller/` REST 接口
- `service/` 业务逻辑
- `mapper/` MyBatis-Plus Mapper（XML 在 `src/main/resources/mapper/`）
- `entity/` 实体（与 `schema.sql` 中表一一对应）
- `config/` 拦截器、异常处理、调度任务、数据初始化
- `util/` JWT、验证码、钉钉工具
