# IMPLEMENTATION_STATUS.md — 物料管理系统 (Material Management System)

Repository: `dsh-native-01` (backend Spring Boot 2.7 + MyBatis-Plus/MySQL, frontend Vue3 + Vite + Element Plus).
This file tracks what is implemented, what remains, known blockers/limitations, and how to build & test.

---

## 1. Test / build commands and actual results (verified in this environment)

| Command | Result |
|---|---|
| `cd backend && mvn -q -DskipTests compile` | ✅ BUILD SUCCESS |
| `cd backend && mvn -q test` | ✅ **33 tests, 0 failures, 0 errors** (unit + H2 integration) |
| `cd frontend && npm install --cache ./.npm-cache && npm run build` | ✅ `✓ built` (vite 8/rolldown) |
| `cd backend && mvn spring-boot:run` | Needs MySQL (see blockers). Spring context verified via `BmsApplicationTests` (H2 profile) |

Notes:
- npm's default cache (`/mnt/d/agent-lab/cache/npm`) is read-only in this environment — installs must pass `--cache` to a writable path (the repo-local `.npm-cache` is git-ignored).
- MySQL is **not installed** on this machine; all DB-backed behavior is verified against **H2 (MySQL mode)** via the `test` Spring profile (`src/test/resources/application-test.properties` + `schema-h2.sql`). The production schema is `backend/src/main/resources/schema.sql` and is auto-applied on startup (`spring.sql.init.mode=always`, idempotent DDL).

---

## 2. What is implemented (completed items)

### 系统登陆 (Login)
- 用户名+密码登录（验证码、回车快速登录、登录限流 10次/分钟→锁定5分钟）
- 注册：用户名/密码(8~20位，大小写字母/数字/特殊符号至少3类)/确认密码/真实姓名/手机号/部门/验证码；**注册时实时校验用户名/手机号唯一性**（`GET /user/check`，前端 blur 校验）；用户名、手机号全局唯一；**新用户强制 engineer 角色（修复了客户端可传 role=admin 的越权漏洞）**
- 权限验证：**每次请求从数据库重新校验用户状态、角色、权限集合**（LoginInterceptor + PermissionService），禁用用户立即失效、权限变更实时生效；`@RequireRole` + 新增 `@RequirePermission`（菜单/按钮级）
- 记住密码：加密(混淆)存储密码到 localStorage，自动填充账号+掩码密码；取消勾选清除
- 登录日志：记录账号/IP/设备/时间(秒)/结果；管理员筛选；**导出 Excel**；用户表新增 `last_login_time`
- 钉钉扫码登录：真实 OAuth2 实现 + **演示模式**（未配置真实凭据时返回确定性模拟登录，不调用外部 API）

### 入库管理
- 入库单列表按单号/供应商/状态/关键词/类型/**入库时间范围/明细物料名称**筛选；**采购入库(inType=1)与退库入库(inType=2)分离**；**退库原因(returnReason)持久化**；入库/出库单支持**按单导出Excel**
- **批量审核入库**（`POST /inbound/batchAudit`，带按钮权限）
- 采购/退库入库：手动增删明细（名称/封装/value/规格/厂家/批次/数量/存放货位/备注完整持久化，修复了原实现只存 materialId/code/num/batchNo 导致的数据丢失）；**确认入库时新物料自动建档并生成物料编码**（MTR-yyyyMMdd-XXXX），自动增加库存（失败即抛异常回滚），写入不可删除的入库记录（含物料编码/名称/批次/货位/供应商）
- 读取钉钉申请单：`POST /inbound/dingtalk/pull`（演示模式下生成确定的示例单，生产可对接真实钉钉审批实例）
- 入库记录：筛选（单号/关键词/时间）、**导出 Excel**、打印

### 出库管理
- 出库草稿→钉钉审批→确认出库/驳回；出库单号 `JyyyyMMdd-xxx` 格式
- 库存占用/释放：草稿锁定（`stock-lock_stock>=num` 守卫防超额占用）、确认扣减+释放、驳回释放；数量>0 校验；空明细拦截
- **前端直接提交时自动创建草稿**（修复“发起钉钉出库审批”必失败问题）
- 数据范围：self/dept/all（按角色配置，self 只显示本人单据）
- 出库记录：单号/物料(编码/名称/批次关键词)/时间筛选、部门、**导出 Excel**；**出库后剩余库存≤物料阈值（minStock，默认5件）自动钉钉通知库管员**
- BOM（需求 2.3.2）：`POST /outbound/bom/match`（服务端逐项匹配：库存充足/不足/缺料/被占用，含已出库物料备注最近出库单号）、`POST /outbound/bom/import`（保存备料计划单，记录 BOM 版本）、`GET /outbound/bom/plan`（历史查询）；前端生产领料页已接入

### 库存管理
- 物料检索/库存查询：编码/名称/封装/仓库/关键词；后端计算物料状态（空闲/出库中/缺货）返回；**物料管理页（新增/编辑/删除物料，含成本/阈值/货位）**
- 库存预警：每日 2:00 扫描（低库存/超储）、同日去重、标记已处理（处理人/方式/时间）、手动扫描（仅管理员）、**钉钉通知（演示模式写日志，配置 webhook 后真实发送）**、**补货申请**（落库 purchase_request + 通知采购员）；预警列表对库管员/采购员/主管开放查看
- 同步CIS元件库：`POST /cis/sync/full|incremental` + 同步日志查询；**可配置适配器**（`cis.endpoint`），演示模式下确定性模拟，不调用外部系统
- 库存流水：合并入库/出库，按物料/时间/类型筛选，**导出 Excel**

### 报表统计
- 库存明细（含导出）、入库统计（按时间/物料[总数/次数/平均批次]/供应商，柱状图）、出库统计（按时间/物料/领料部门）、呆滞物品（**含库存金额 = stock×material_cost**）
- 导出：所有报表导出 xlsx，文件名 `报表名称_yyyyMMdd_HHmmss`，**>50000 行自动打包 zip**；导出走带 Authorization 的 blob 下载（修复 window.open 401）

### 系统管理
- 用户管理：增删改查、启用/禁用、角色分配、**管理员新增用户**（`POST /user/add`，初始密码+角色）、重置密码（Abc@12345）、**删除用户前检查未完成单据**、**批量导入用户（Excel，`POST /user/import`）**、**导出用户 Excel**、上次登录时间
- 角色权限：**真实落库的自定义角色 + 菜单/按钮权限勾选 + 数据范围**（sys_role/sys_permission/sys_role_permission/sys_user_role），保存即实时生效；前端 RoleManage 接入真实 API
- 数据备份：手动全量 mysqldump、**自动备份调度**（每周日 2:00 全量 + 每日 2:30 增量，cron 可配置）、**保留周期自动清理**（默认30天）
- 系统日志：记录注册/登录/登出/改密/增删改用户/出入库/导出/备份/同步等；筛选、**导出 Excel**；**日志保留期自动清理**（默认365天）
- 密码修改：本人修改（服务端完整强度校验）、管理员强制重置

### 安全加固（本轮新增）
- **操作人取自登录令牌**：确认入库/出库不再接受客户端传入的 operUser，杜绝审计伪造（H3）
- **DingTalk 回调驳回释放库存**：出库回调 refuse 走 rejectOut 释放占用，避免锁定滞留（C3）
- **草稿占用自动清理**：DraftLockCleanupScheduler 每日清理超过 TTL（默认7天，可配）的未提交出库草稿并释放占用（H5）
- **登录限流加固**：限流键 = IP+用户名，验证码错误不再累计锁定，防一人锁死全站（M12）
- **备份密码不进进程列表**：mysqldump 改用 MYSQL_PWD 环境变量；备份增加10分钟超时（M13）
- **DingTalk OAuth state 绑定 redirectUri + 10分钟过期**（M9）
- **CORS 白名单化**：默认仅本机开发端口，`app.cors.allowed-origins` 可配置，不再 通配符+credentials（H1）
- **安全响应头**：X-Frame-Options/X-Content-Type-Options/Referrer-Policy/Cache-Control（L1）
- **管理员自我保护**：不能自我降权/禁用（M2）；**JWT 默认密钥告警**（H2）
- **其他**：验证码 SecureRandom（L6）、钉钉 token/响应日志脱敏（L4）、SQL 日志级别降为 INFO（L3）、Knife4j 可配置开关（L2）
- 生产部署仍需：真实钉钉回调**签名/加解密验签**（C3，需企业应用凭据）、JWT_SECRET 强随机密钥、数据库强口令

### 基础设施 / 质量
- **完整数据库 schema**（18+ 张表，与实体/Mapper SQL 逐条对齐）：`backend/src/main/resources/schema.sql`
- 安全修复：注册角色注入、JWT 每请求 DB 复核、DingTalk 回调幂等/状态校验、统计 SQL 分组白名单、CORS 说明、mysqldump 参数说明（见 blockers）
- 乐观锁插件注册；原子库存 SQL 守卫；`version`/`lock_stock` 空值兜底
- 自动化测试 33 个：单元（JwtUtil/CaptchaUtil/PasswordPolicy/LoginRateLimiter/Result/BackupController.extractDbName）+ H2 集成（注册校验/入库建档加库存/出库占用扣减解锁/驳回解锁/库存不足拦截/BOM 匹配/预警扫描/CIS 同步/鉴权 401/403/禁用用户/DingTalk回调驳回释放库存）

---

## 3. Remaining / incomplete items (accurately documented)

1. **真实外部集成未验证（仅可配置适配器 + 演示模式）**：
   - 钉钉：审批发起/扫码登录/消息通知 —— 演示模式返回模拟结果；真实调用需配置 `dingtalk.app.key/secret/corp.id/agent.id/process-code/notify.webhook` 且 `dingtalk.mock.enabled=false`。
   - CIS 元件库：演示模式为确定性模拟；真实上报需配置 `cis.endpoint`（HTTP JSON 协议按实际 CIS 接口调整）。
   - 未声明任何“已与真实钉钉/CIS 联调通过”。
3. **入库/出库单 Excel 导出的“打印入库明细单”**：打印用浏览器 window.print；PDF 导出未实现（无 PDF 依赖）。
4. **备份调度 cron 读取**：调度器使用 `@Scheduled(cron="${backup.full.cron:...}")`（属性/环境变量），数据库里可改配置项但需重启生效（已在前端提示）；保留周期清理为备份后执行。
5. **库存流水分页**：仍为内存合并后分页（记录量大时建议改 DB 分页）。
6. **呆滞物品统计**：`DATEDIFF` 为 MySQL 语法，H2 测试未覆盖该查询（统计其余查询均可用）。
7. **物料类别/部门管理**：部门为用户字符串字段，无独立部门/类别表（需求为“选择式级联”，前端用扁平搜索代替）。
8. **数据范围**：出库单查询实现了 self 过滤（按角色 data_scope）；入库/流水等其他列表的按角色数据范围完整过滤未铺开。
9. **钉钉回调签名校验**：回调端点未做签名/加密验签（公开端点），生产建议对接官方验签。
10. **系统日志保留/备份保留 UI**：保留天数在 DB 配置可改，无专门管理页面。
11. `xlsx@0.18.5` 存在已知 CVE（前端依赖，建议升级或接受风险）；Knife4j 建议生产关闭。

---

## 4. Blockers

- **本机无 MySQL**：无法在本环境启动真实 MySQL 服务做端到端验证；用 H2(MySQL 模式) 覆盖全部 SQL 流程，生产部署请用 `schema.sql` 建库（含 `CREATE DATABASE IF NOT EXISTS bms`，JDBC 已加 `createDatabaseIfNotExist=true`）。
- **共享 git 目录只读**：原 worktree git 元数据目录（baseline/.git）为只读文件系统，无法写入提交；已在工作区内 `git init` 建立本地可提交仓库用于 checkpoint 提交（首个提交包含全部当前改动）。
- **npm 默认缓存只读**：需 `--cache` 指定可写路径（已加入说明与 .gitignore）。

---

## 5. 部署说明 (quick start)

```bash
# 后端
cd backend
mvn -q -DskipTests package
java -jar target/bms-1.0.0.jar            # 默认连 localhost:3306/bms，自动建库建表
# 环境变量可覆盖：DB_URL/DB_USERNAME/DB_PASSWORD/JWT_SECRET/DINGTALK_*/CIS_ENDPOINT

# 前端
cd frontend
npm install --cache ./.npm-cache
npm run dev                                # http://localhost:5173，/api 代理到 :8080
# 或 npm run build

# 默认账号：admin / Abc@12345（首次登录后请修改）
```
