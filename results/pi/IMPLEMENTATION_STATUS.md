# 实施状态报告 (IMPLEMENTATION STATUS)

> 物料管理系统（Spring Boot 2.7 + Vue 3 + MyBatis-Plus + MySQL / H2测试）
> 本文件按需求逐项给出完成状态、环境变量、种子账号、测试/构建命令与已知限制。

## 构建与测试命令

| 命令 | 结果 |
|------|------|
| `cd backend && mvn -q -DskipTests compile` | ✅ 通过 |
| `cd backend && mvn test` / `mvn package` | ✅ 通过（40 个测试全绿，产出 `target/bms-1.0.0.jar`） |
| `cd frontend && npm install` | ✅ 通过 |
| `cd frontend && npm run build` | ✅ 通过 |

测试运行于 H2 内存库（`MODE=MySQL;DATABASE_TO_LOWER=TRUE`），无需 MySQL。
启动（开发）：`mvn spring-boot:run`；前端：`npm run dev`（`/api` 代理到 8080）。

## 种子账号

| 账号 | 密码 | 角色 | 说明 |
|------|------|------|------|
| admin | Admin@123456 | admin | 管理员（全部权限），由 `DataInitializer` 幂等创建 |
| warehouse | Warehouse@123456 | warehouse | 库管员（仅当用户表为空时创建） |

角色/权限种子：admin(全部)、warehouse(除系统管理菜单)、engineer(生产领料/物料检索/库存查询/密码修改)、
purchaser、inspector、manager。`sys_user.role` 关联 `sys_role.role_code`，`sys_role_permission` 关联权限。

## 环境变量参考

| 变量 | 默认值 | 说明 |
|------|--------|------|
| DB_URL / DB_USERNAME / DB_PASSWORD | jdbc:mysql://localhost:3306/bms / root / 空 | 数据库 |
| JWT_SECRET | 内置 | JWT 签名密钥 |
| DINGTALK_APP_KEY / DINGTALK_APP_SECRET 等 | demo-* | 钉钉应用 |
| DINGTALK_CALLBACK_TOKEN | demo-callback-token | 钉钉回调验签 Token |
| DINGTALK_MODE | mock | mock=仅记日志；robot=推机器人 webhook |
| DINGTALK_WEBHOOK_URL | 空 | 钉钉机器人 webhook |
| DEFAULT_PASSWORD | Sys@123456 | 管理员重置/导入用户的默认密码（满足密码策略：8-20位、≥3类字符） |
| SYS_LOG_RETENTION_DAYS | 365 | 日志保留天数 |
| CIS_SYNC_MODE | mock | mock=仅记同步日志；否则调用 CIS_SYNC_URL |
| CIS_SYNC_URL | http://localhost:9090/cis | CIS 系统地址 |
| sys.role.cache.ttl.ms | 30000 | 角色实时校验缓存 TTL |

## 需求逐项状态

### 2.1 系统登陆
| 需求项 | 状态 | 说明 |
|--------|------|------|
| 用户注册/登录 | ✅ 完成 | 登录含验证码、限流；注册使用 `RegisterDTO`（只接收 username/password/confirmPassword/realName/phone/dept，id/status/role/dingtalkUnionId 无法批量赋值，防提权/防伪造）；用户名/手机号全局唯一（`sys_user` 唯一索引 + 服务层预检 + DuplicateKeyException 友好提示）；密码策略统一（8-20位、≥3类字符，注册/改密共用 `PasswordPolicyUtil`）；注册需验证码且两次密码一致；`/user/checkUsername`、`/user/checkPhone` 实时唯一性检查 |
| 权限验证 | ✅ 完成 | 后端 `RoleInterceptor` 每请求按 userId 查库实时校验角色（忽略 JWT 角色声明，防越权）；管理员侧角色/权限变更**立即失效缓存**（`/user/update`、`/user/delete`、`/role/*/permissions` 主动 evict），其余场景 30s TTL 近似实时；所有请求（含无 `@RequireRole` 的接口）的 `role` 请求属性均取自 DB；前端菜单/按钮按权限渲染 |
| 记住密码 | ✅ 完成（混淆级） | XOR+btoa 混淆存储于 localStorage（**非加密**，见已知限制）；勾选保存、取消清除、自动填充 |
| 登陆日期 | ✅ 完成 | `sys_login_log` 记录账号/IP/设备/时间/结果；管理员可在系统日志查询、导出登录日志 Excel |

### 2.2 入库管理
| 需求项 | 状态 | 说明 |
|--------|------|------|
| 入库管理（采购入库） | ✅ 完成 | 单据列表按单号/供应商/状态/时间查询；库管员/管理员确认入库（自动生成物料编码 MTR-yyyyMMdd-xxxx）；自动增加库存；写入入库记录（物料编码/名称/批次/数量/货位）；支持批量审核 `POST /inbound/batchConfirm`；库管员填写货位；同批次/不同位置分别记录；入库类型 PURCHASE/RETURN 与退库原因持久化 |
| 退库入库 | ✅ 完成 | 退库单维护（类型=RETURN、退库原因）；确认退库走同一确认流程 |
| 入库记录 | ✅ 完成 | 自动写入 `in_record` 不可删除；按单号/物料/批次/时间筛选；导出 Excel（`GET /inRecord/export`，过滤参数与分页一致：startDate/endDate/keyword/billNo，文件名 报表名称+yyyyMMdd_HHmmss.xlsx）、打印 |
| 钉钉回调 | ✅ 完成 | 回调增加 `X-Callback-Token`/`token` 验签（`MessageDigest.isEqual` 常量时间比较，403 拒绝无效）；`/inbound/updateStatus` 端点已移除，回调只走服务层状态守卫（`approveFromCallback`/`refuseFromCallback`） |

### 2.3 出库管理
| 需求项 | 状态 | 说明 |
|--------|------|------|
| 出库管理 | ✅ 完成 | 库管员选择出库单、确认出库（扣库存+释放锁定+写 `tb_out_record`）；驳回释放锁定并通知申请人；确认/驳回仅 admin/warehouse；确认/驳回为**原子状态流转**（`UPDATE ... WHERE order_status=0`，防 TOCTOU）；确认时按**总库存**校验（lockStock 已含本单占用，多单共同预订同一物料均可确认），真正并发保护由原子扣减 SQL `WHERE stock>=num` 兜底 |
| 生产领料 | ✅ 完成 | 两种模式：导入BOM表（服务端 POI 解析 `/outbound/bom/import`，容错表头）与配置BOM清单（类别→物料→规格联动选择）；`/outbound/bom/match` 逐项匹配库存状态：充足/不足/缺料/被占用（可用=stock-lock_stock）；`/outbound/bom/plan` 保存备料计划单+生成出库草稿；`/outbound/bom/history` 历史 BOM 模糊查询与复用（重新匹配）；保存时记录 BOM 版本；BOM 导入/匹配/计划/历史对登录用户开放（工程师发起生产领料），`/outbound/saveDraft|saveOrder|editDraft` 对登录用户开放且校验单归属（工程师只能提交自己的草稿），确认/驳回仍仅 admin/warehouse；工程师可通过备料计划**占用库存**（需求固有，备料即锁定） |
| 出库记录 | ✅ 完成 | `tb_out_record` 自动记录；按单号/物料/时间筛选；导出 Excel（`GET /outRecord/export`，过滤参数与分页一致：outboundCode/materialId/startTime/endTime，文件名 报表名称+yyyyMMdd_HHmmss.xlsx） |
| 数据范围（工程师） | ✅ 完成 | 非 admin/warehouse 角色访问 `inbound/outbound` 单据列表/详情、`in/outRecord` 记录分页时，按 `applyUser`/`in_user`/`out_user` == 当前登录人过滤（role 属性由 RoleInterceptor 从 DB 实时解析），工程师只能看到自己的领料单与记录，看他人详情返回 403 |
| 钉钉出库回调 | ✅ 完成 | 驳回回调走 `rejectOut`（原子 0→2 + **释放锁定库存**，修复此前锁定泄漏）；同意走 `approveFromCallback`（仅 0→0，防旧回调回写已处理单据）；验签为 `MessageDigest.isEqual` 常量时间比较 |
| 库存不足通知 | ✅ 完成（mock级） | 出库锁定校验 + 每日库存预警扫描 ≤ 最小库存/≥ 最大库存 → 生成预警并钉钉通知库房管理员/采购员 |

### 2.4 库存管理
| 需求项 | 状态 | 说明 |
|--------|------|------|
| 物料检索/库存查询 | ✅ 完成 | 按编码/名称/封装/规格查询；显示库存、占用（lock_stock）、可用、最低/最高库存、状态（空闲/占用/缺货）；物料 CRUD 界面（新增/编辑/删除，含 minStock/maxStock/materialCost/locationNo/厂家字段），删除仅 admin；新增/编辑接口**忽略请求体中的 stock/lockStock/version**（库存只能经出入库流程变更，前端编辑弹窗中库存为只读并提示）；编辑时服务端从 DB 重读 version 保证乐观锁生效 |
| 库存预警 | ✅ 完成 | 每日凌晨扫描（`StockAlertScheduler`）；低库存(type=1)/超储(type=2) 同一天不重复（**按物料+类型+日期去重，与 handled 无关**，标记处理后不再重复告警）；标记处理（处理人/方式）；手动扫描仅 admin；补货申请真实接口（`/replenishment/apply`，admin/warehouse/**purchaser** 可发起，缺货数量预填、厂家联系方式+采购数量）；补货列表与处理仅 admin/warehouse |
| 同步CIS元件库 | ✅ 完成（mock级） | `/cis/sync/full`、`/cis/sync/incremental`、`/cis/sync/log/page`（admin）；增量同步以最近成功同步时间为界；confirmIn/confirmOut 自动触发增量同步（异步 fire-and-forget）；mock 模式仅写 `cis_sync_log` |
| 库存流水 | ✅ 完成 | SQL 层 UNION 合并入库/出库记录，服务端过滤（类型/物料编码/时间/关键词）+ 分页；导出 Excel（`/stockFlow/export`）；page/export 仅 admin/warehouse |

### 2.5 报表统计
| 需求项 | 状态 | 说明 |
|--------|------|------|
| 库存明细 | ✅ 完成 | 物料库存明细列表；导出 `/statistics/exportInventory` |
| 入库统计 | ✅ 完成 | 按日/周/月/年柱状图；`/statistics/inboundBySupplier` 供应商维度统计 |
| 出库统计 | ✅ 完成 | 折线图；`/statistics/outboundByDept` 领料部门维度统计 |
| 呆滞物品 | ✅ 完成 | 超 90/180/365 天无出库的物料清单：最后出库时间、当前库存、库存金额；分页展示；导出 |
| 导出报表 | ✅ 完成 | 报表名称+yyyyMMdd_HHmmss.xlsx；>50000 行自动打包 zip（内含 xlsx）；前端统一 Blob 下载（带 Authorization），已消除全部 `window.open` 导出 |

### 2.6 系统管理
| 需求项 | 状态 | 说明 |
|--------|------|------|
| 用户管理 | ✅ 完成 | 增删改查、启停、重置密码（默认密码可配置）；删除时校验存在未完成入库/出库单据则拦截（"该用户存在未完成单据，请先转移或处理"）；批量导入/导出用户 Excel（`/user/import`、`/user/export`）；新增用户走 `/user/add`（可指定角色，注册接口强制 engineer 不受影响） |
| 角色权限 | ✅ 完成 | `/role/list`、`/role/permission-tree`（嵌套树）、`/role/{id}/permissions`、`PUT /role/{id}/permissions`（覆盖保存+数据范围）、角色 CRUD；前端角色列表+权限树勾选+数据范围下拉；**管理员侧权限变更立即生效**（`savePermissions`/角色增删改主动 `evictAll`，用户角色/状态变更主动 evict 该用户），其余场景 30s TTL 近似实时 |
| 数据备份 | ✅ 完成 | 手动全量备份（mysqldump）；自动备份策略：周日夜2:00全量、每日凌晨3:00增量（简化：同为 mysqldump 全表导出并标注增量，见已知限制）；**数据库密码通过 `MYSQL_PWD` 环境变量传给 mysqldump**（不再出现在命令行参数）；保留周期配置与过期清理；备份记录列表；mysqldump 不可用时记录 FAILED 不崩溃 |
| 系统日志 | ✅ 完成 | 登录日志+操作日志（登录/注册/增删改用户/权限变更/入库确认/批量审核/出库确认/驳回/补货/CIS同步/备份/导出均记录）；保留天数可配置（默认365），每天4点自动清理；导出操作日志/登录日志 Excel |
| 密码修改 | ✅ 完成 | 登录用户修改本人密码（**与注册共用密码策略**：8-20位、≥3类字符，`PasswordPolicyUtil`）；管理员重置密码（默认密码 `Sys@123456`） |

## 测试覆盖（Phase 11 + 安全回归）

`backend/src/test/java/com/koolearn/bms/service/`（共 40 个用例，全部通过）：

- `RegisterPrivilegeTest`（6）：注册提权被强制为 engineer；重复用户名/手机号拒绝；空用户名/姓名/非法手机号拒绝
- `InboundConfirmStockTest`（3）：确认入库库存增加；InRecord 写货位/物料名/编码；编码自动生成；重复确认拒绝；多明细各写记录
- `OutboundLockConfirmRejectTest`（8）：saveDraft 锁定；可用不足抛异常；confirmOut 扣减+解锁+写记录；rejectOut 解锁；**本单占用存在但总库存足够时可确认**；**总库存真正不足时拦截**；**两单共同预订同一物料均可确认**；非草稿状态禁止编辑
- `StockAlertScanTest`（4）：低库存预警生成且同日不重复；超储 type=2；正常库存不生成；**标记处理后当天不再重复告警**
- `BomMatchTest`（2）：充足/不足/缺料/被占用状态判定与补货数量
- `RolePermissionTest`（5）：MockMvc 工程师访问 admin 接口 403；无 token 401；**管理员通过 /user/update 真实变更角色后下一请求即时生效（不再手动 evict）**；库管员权限边界；**管理员禁用用户后 403**
- `CallbackAndExportTest`（4）：**出库驳回回调释放锁定库存+状态=2**；**/inbound/updateStatus 已移除（404）**；**/inRecord/export、/outRecord/export 返回 200 xlsx**
- `SecurityRegressionTest`（5）：**注册请求体携带 id/role/status 被忽略（RegisterDTO 防批量赋值）**；两次密码不一致拒绝；**changePwd 密码策略（弱密码拒绝、合规密码成功）**；**物料更新忽略请求体 stock/lockStock/version**；入库草稿非待审批禁止编辑
- `EngineerDataScopeTest`（2）：**工程师查询出库单只见自己的**；**工程师查看他人单据 403**

## 已知限制（Known Limitations）

1. **记住密码为混淆存储**（XOR + base64），非强加密；仅用于本地便利，符合需求描述但安全等级有限。
2. **数据范围（dataScope）为部分实现**：`/user/info` 返回数据范围，角色可配置 all/dept/self；业务侧按"非 admin/warehouse 只看自己的单据/记录"强制过滤（工程师数据范围已生效），但 dept/all 等更细粒度范围尚未按部门实现。
3. **增量备份为简化实现**：无 binlog，增量备份实际仍执行 mysqldump 全表导出并标注 incremental（需求明确记录的限制，文档化于 `BackupServiceImpl`/`BackupScheduler`）。
4. **钉钉/CIS 默认 mock**：`dingtalk.mode=mock`、`cis.mode=mock` 时仅写日志/操作日志，不做真实 HTTP 调用；真实接入需配置 webhook/CIS 地址（无线上环境集成验证）。
5. **工程师出库提交**：`/outbound/saveDraft|saveOrder|editDraft` 对登录用户开放（工程师提交领料申请/备料计划，可占用库存，为需求固有行为），但只能操作自己的单；confirm/reject 仍仅 admin/warehouse；BOM 导入/匹配/计划/历史开放给登录用户。
6. **删除用户未处理预警校验**：预警无用户归属字段，删除拦截仅覆盖未完成入库/出库单据。
7. **导出 >50000 行 zip** 逻辑已实现，未用真实 5 万行数据压测（inRecord/outRecord 导出当前为单文件 xlsx，未做 zip 分卷）。
8. **MySQL 未运行**：全部验证基于 H2（MODE=MySQL）；MySQL 8 上的行为基于 SQL 兼容性设计（含 `SELECT ... FOR UPDATE` 锁定读重试、`MYSQL_PWD` 备份、唯一索引），未做真实库回归。
9. **knife4j/springfox**：`ant_path_matcher` 已生效，上下文启动正常，无需移除依赖。
10. **种子权限与后端 @RequireRole 对齐**：采购员不再持有报表/预警处理/库存流水权限（后端为 admin/warehouse），补货申请允许 purchaser；报表菜单从 purchaser/manager/inspector 移除；manager 仍持有全部非系统菜单（含确认/驳回等按钮权限），但后端确认/驳回等动作仍按 @RequireRole 强制 admin/warehouse（前端按钮不据此渲染，实际调用会 403，属菜单可见性与动作权限的既有差异）。
11. **JWT_SECRET 环境变量**：若设置但长度 < 32 字节，启动直接抛明确错误（fail-fast）；未设置时使用内置默认密钥（生产应覆盖）。
12. **乐观锁重试**：库存重试的每次尝试使用 `SELECT ... FOR UPDATE` 锁定读（当前读）读取最新版本，规避 MySQL REPEATABLE READ 快照导致的重试无效；H2 测试环境同样通过。
13. **sys_user 唯一索引**：`schema.sql` 在 `CREATE TABLE` 内联声明 username/phone 唯一索引，新库生效；已存在的旧库不会自动加索引（服务层预检 + DuplicateKeyException 兜底仍有效）。

## 未完成项

- 无（Phases 1-11 均已实现并通过构建/测试）。后续增强方向见"已知限制"。

## 主要变更文件（后端）

- 配置：`application.properties`（默认密码 Sys@123456）、`schema.sql`（sys_user 唯一索引）、`data.sql`、`CorsConfig`、`RoleInterceptor`（实时角色+缓存+数据范围 role 属性）、`MybatisPlusConfig`（乐观锁）、`DataInitializer`（种子与 @RequireRole 对齐）、`StockAlertScheduler`、`BackupScheduler`、`SysLogCleanupScheduler`、`GlobalExceptionHandler`（重复键友好提示）
- 控制器：`LoginController`（RegisterDTO、changePwd 策略、角色缓存 evict）、`InboundOrderController`（移除 updateStatus、回调状态守卫、数据范围）、`OutboundOrderController`（回调解锁、数据范围、保存/提交开放）、`InRecordController`/`OutRecordController`（新增 export）、`MaterialController`（防 stock/lockStock/version 批量赋值）、`StockFlowController`/`StockAlertController`/`ReplenishmentController`（角色收紧）、`RoleController`（权限变更 evictAll）、`StatisticsController`、`CisController`、`BackupController`、`SysLogController`、`LoginLogController`、`BomController`
- 服务：`UserServiceImpl`（注册强制 engineer + 策略共用）、`InboundOrderServiceImpl`（原子确认/回调守卫/editDraft 状态守卫）、`OutboundOrderServiceImpl`（确认按总库存 + 原子流转 + 回调驳回解锁）、`MaterialServiceImpl`/`MaterialStockRetryHelper`（FOR UPDATE 当前读重试）、`StockAlertServiceImpl`（按天去重与 handled 无关）、`BackupServiceImpl`（MYSQL_PWD）、`DingTalkLoginService`（state 过期清理+有界）、`RoleServiceImpl`、`CisSyncServiceImpl`、`DingTalkNotifierImpl`、`BomMatchServiceImpl`
- DTO/Util：`RegisterDTO`（防批量赋值）、`PasswordPolicyUtil`（密码策略共用）、`JwtUtil`（JWT_SECRET 长度校验）
- 实体/Mapper：User、Material(+厂家字段)、InboundOrder(+inType/returnReason)、InRecord(+code/name/location)、Role/Permission/RolePermission、BomHeader/BomItem、CisSyncLog、Replenishment、BackupConfig/BackupRecord、StockFlowMapper(+XML)、OutboundOrderMapper/InboundOrderMapper/InRecordMapper/MaterialMapper（原子状态流转/锁定读/导出查询）

## 主要变更文件（前端）

- `src/utils/request.js`（downloadBlob 401/403/404 文案）、`src/utils/permission.js`（hasPerm + firstPermittedRoute）、`src/router/index.js`（登录后跳首个有权限路由 + 兜底）
- `src/api/index.js`（移除 inbound updateStatus）
- 视图：login/index.vue（登录/钉钉登录跳转首个有权限路由、注册传 confirmPassword）、inventory/MaterialSearch.vue（库存只读+不提交 stock/lockStock）、outbound/ProductionPicking.vue（工程师领料流程）、inbound/*、report/*（导出改造）、layout/Sidebar.vue 等

## 最终验证结果（评审修复后实跑）

### 后端测试（H2 内存库，无 MySQL）

命令：`cd backend && mvn test`

```
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0 - in com.koolearn.bms.BmsApplicationTests
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0 - in com.koolearn.bms.service.BomMatchTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0 - in com.koolearn.bms.service.CallbackAndExportTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0 - in com.koolearn.bms.service.EngineerDataScopeTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0 - in com.koolearn.bms.service.InboundConfirmStockTest
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0 - in com.koolearn.bms.service.OutboundLockConfirmRejectTest
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0 - in com.koolearn.bms.service.RegisterPrivilegeTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0 - in com.koolearn.bms.service.RolePermissionTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0 - in com.koolearn.bms.service.SecurityRegressionTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0 - in com.koolearn.bms.service.StockAlertScanTest
[INFO] Tests run: 40, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

命令：`cd backend && mvn -q -DskipTests package` → 产出 `target/bms-1.0.0.jar`（59,396,045 字节）✅

### 前端构建

命令：`cd frontend && npm run build`

```
✓ built in 8.81s
```
（仅有 rolldown 对第三方库 `/* #__PURE__ */` 注释位置的提示性警告，无错误）✅
