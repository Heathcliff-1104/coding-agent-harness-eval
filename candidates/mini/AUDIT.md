# Material Management System — Audit Report

Audited against `requirements/requirements.md` (功能框架 v1). Scope: backend (Spring Boot 2.7 / MyBatis-Plus / MySQL) and frontend (Vue 3 + Vite + Element Plus). No fixes were applied; this file only records findings.

Priority legend: **CRITICAL** (security / broken core flow), **HIGH** (major requirement gap or guaranteed failure), **MEDIUM** (significant gap/bug), **LOW** (polish / minor).

---

## CRITICAL

### C1. Registration allows client-supplied `role`, enabling privilege escalation to admin
- **Requirements:** 2.1.1 (新用户注册后系统自动分配工程师权限，最低权限), 2.1.2 (防止越权)
- **Evidence:** `backend/src/main/java/com/koolearn/bms/service/impl/UserServiceImpl.java`
  ```java
  user.setRole(user.getRole() != null ? user.getRole() : "engineer");
  ```
  The `role` field is taken verbatim from the request body. A user can `POST /user/register` with `{"username":"x","password":"...","role":"admin"}` and obtain an admin JWT. The frontend even exposes `admin` as a selectable role in `frontend/src/views/system/UserManage.vue`.
- **Severity:** CRITICAL
- **Recommended bounded task:** In `UserServiceImpl.register`, always overwrite `user.setRole("engineer")` regardless of request body; add a server-side DTO that excludes `role`. Add a test asserting a registered user cannot set a non-engineer role.

### C2. Permission checks rely on a JWT role claim fixed for 7 days; role changes are NOT real-time
- **Requirements:** 2.1.2 (用户每次请求后端接口时，重新校验用户权限), 2.6.2 (权限变更实时生效，用户无需重新登录)
- **Evidence:** `backend/src/main/java/com/koolearn/bms/config/LoginInterceptor.java` reads `role` from JWT claims; `RoleInterceptor.java` uses `request.getAttribute("role")`; `util/JwtUtil.java` issues tokens with 7-day expiry. Frontend permissions come from `localStorage.role` (`frontend/src/utils/permission.js`). When an admin edits a user's role, existing tokens still carry the old role until re-login.
- **Severity:** CRITICAL (privilege/access-control correctness)
- **Recommended bounded task:** Re-query the user's current role/status from DB per request (or short-lived tokens + refresh), and reject requests for disabled users. Frontend should load permissions from `/user/info` instead of localStorage.

---

## HIGH

### H1. Post-login redirect target `/inbound` does not exist; non-admin roles hit an infinite navigation guard loop
- **Requirements:** 2.1.1 (登录界面)
- **Evidence:** `frontend/src/views/login/index.vue` lines 166 and 236 `router.push('/inbound')` / `router.replace('/inbound')`; `frontend/src/router/index.js` defines no `/inbound` route (only `/inbound/purchase` etc.), and its `beforeEach` guard redirects an unauthorized `/inbound/purchase` to itself (`next('/inbound/purchase')`). For `engineer`, `purchaser`, `inspector`, and `manager` roles `canAccess('/inbound/purchase')` is false (`frontend/src/utils/permission.js`), so the guard loops forever after login. Also the default route redirect `/` → `/inbound/purchase` is not accessible to those roles.
- **Severity:** HIGH
- **Recommended bounded task:** After login, route to a role-appropriate default page (e.g., first allowed menu). Add a catch-all route and guard logic that, when the target is forbidden, redirects to the first allowed menu instead of the same path. Add a test for each role's default landing route.

### H2. Frontend BOM import/match API has no backend implementation; BOM flows are mock
- **Requirements:** 2.3.2 (导入BOM表、配置BOM清单、模糊查询、保存备料计划单、记录BOM版本、重复BOM匹配历史)
- **Evidence:** `frontend/src/api/index.js` defines `bomImport: /outbound/bom/import` and `bomMatch: /outbound/bom/match`, but `backend/.../controller/OutboundOrderController.java` has no such mappings. In `frontend/src/views/outbound/ProductionPicking.vue`, BOM matching is done client-side by paging materials; `saveAsPlan()` only shows a toast ("已保存为备料计划单"), no persistence. BOM version/history not modeled anywhere.
- **Severity:** HIGH
- **Recommended bounded task:** Add backend endpoints for BOM import/match and `备料计划单` persistence (entity + table), record BOM version and match history, and wire the frontend to those endpoints; remove the dead client-side matching or keep it as preview only.

### H3. Outbound order submission is broken: frontend calls `saveOrder` without a draft `id`
- **Requirements:** 2.3.1 (确认出库), 2.3.2 (生产领料/发起钉钉出库审批)
- **Evidence:** `frontend/src/views/outbound/ProductionPicking.vue` `submitBomOutbound()` and `submitConfigOutbound()` call `outboundApi.saveOrder({ outType, applyUser, remark, itemList })` with **no `id`** and no prior draft creation. Backend `OutboundOrderServiceImpl.saveOrder()` (lines ~165-185) does `outboundOrderMapper.selectById(dto.getId())` and throws `"单据统计异常，无法提交审批"` when the draft is absent; it also ignores `dto.itemList` entirely (reads items from DB instead). Result: picking/outbound applications can never be submitted.
- **Severity:** HIGH
- **Recommended bounded task:** Either make `saveOrder` create a new order + items from `itemList` (and submit DingTalk), or make the frontend call `saveDraft` first and pass the returned draft `id`. Add an integration test for the full submit path.

### H4. Manual inbound rows lack `materialId`; confirm-in fails for manually entered materials
- **Requirements:** 2.2.1.1 (支持手动增删物料，填写存放货位), 2.2.1.2 (退库入库)
- **Evidence:** `frontend/src/views/inbound/PurchaseInbound.vue` and `ReturnInbound.vue` add rows with `materialName/packageType/valueData/specModel/manufacturer/batchNo/num/locationNo/remark` but **no `materialId`** (e.g., `addRow()`). Backend `InStorageItem` entity has only `materialId/materialCode/num/batchNo`, so the extra fields are dropped. `InboundOrderServiceImpl.confirmIn()` calls `materialService.addStock(item.getMaterialId(), item.getNum())`; with `materialId == null`, `addStock` returns false and `retryWithBackoff` eventually throws `"库存并发冲突"`. So any inbound order with a manually added material cannot be confirmed.
- **Severity:** HIGH
- **Recommended bounded task:** Add a server-side material create-or-resolve step during `saveOrder`/`saveDraft` (create a `tb_material` row from the item's materialName/packageType/valueData/specModel/locationNo/remark, auto-generate a material code, then set `materialId`). Store `locationNo`/batch per inbound item; enforce the "same type, different batch/location → separate records" rule.

### H5. All report/record Excel exports are broken or placeholder
- **Requirements:** 2.1.4 (导出登录表格), 2.2.2 (导出PDF/Excel), 2.4.4 (导出流水Excel), 2.5.5 (所有报表导出.xlsx; >5万行自动压缩zip), 2.6.1 (批量导入/导出用户), 2.6.4 (日志导出)
- **Evidence:**
  - `frontend/src/views/report/InboundStats.vue`, `OutboundStats.vue`, `Stagnant.vue`, `ExportCenter.vue` use `window.open(...)` to hit `/api/statistics/export...`, `/api/inbound/export/...`, `/api/outbound/export/...`. These bypass the axios interceptor, so **no `Authorization` header** is attached; the backend login interceptor returns 401 for these protected paths.
  - `frontend/src/views/inbound/InRecordList.vue`, `outbound/OutRecordList.vue`, `inventory/StockFlow.vue`, `system/SysLog.vue`, `system/UserManage.vue` show toasts `"导出功能待后端接口"` — no backend endpoint exists for in/out record lists, stock flow, login/system logs, or user list export.
  - Backend exports only cover statistics (`StatisticsController`) and single inbound/outbound order export, and none enforce the `.zip` when >50k rows or the required file naming rule.
- **Severity:** HIGH
- **Recommended bounded task:** Add a shared authenticated download mechanism (axios blob download or token-carrying link), implement server-side Excel export for in/out records, stock flow, login log, system log, user list; implement zip-when->50k and the `报表名+导出时间.zip` naming; replace placeholder toasts with real calls.

### H6. "记住密码" is not implemented as specified
- **Requirements:** 2.1.3 (将加密后的密码存储至浏览器本地存储；下次打开登录页自动填充账号和掩码显示的密码；取消勾选时清除)
- **Evidence:** `frontend/src/views/login/index.vue` only stores `savedUsername` (the username) — never the password, no encryption, no masked auto-fill.
- **Severity:** HIGH (explicit requirement missing)
- **Recommended bounded task:** Store an encrypted/obfuscated password (e.g., AES with a non-extractable key or a reversible obfuscation) in localStorage, auto-fill username + masked password, and clear on uncheck. Add a frontend unit test.

---

## MEDIUM

### M1. Unprotected state-changing endpoints allow any logged-in user to mutate orders
- **Requirements:** 2.1.2 (每次请求后端接口时重新校验用户权限，防止越权)
- **Evidence:** `backend/.../controller/InboundOrderController.java`: `/saveDraft`, `/saveOrder`, `/editDraft/{id}`, `/updateStatus` have no `@RequireRole`. `OutboundOrderController.java`: `/saveDraft`, `/editDraft/{id}`, `/saveOrder`, `/reject/{id}` have no `@RequireRole`. An `engineer` can change order statuses or reject outbound orders.
- **Severity:** MEDIUM (security)
- **Recommended bounded task:** Apply `@RequireRole` to the appropriate methods (e.g., saveDraft/saveOrder for engineer+warehouse+admin; updateStatus/reject for warehouse+admin), and add tests for cross-role calls.

### M2. DingTalk callback endpoints are public and unauthenticated, with no signature verification
- **Requirements:** 2.1.1 (钉钉直接登录), 2.2.1 / 2.3.1 (钉钉审批回调)
- **Evidence:** `InboundOrderController.dingTalkCallback` and `OutboundOrderController.dingTalkCallback` are in `CorsConfig` public paths and accept arbitrary `{instanceId, result}` JSON, updating order statuses without verifying the DingTalk event signature. `DingTalkLoginService` stores OAuth `state` in memory with no expiry cleanup despite `cleanExpiredStates()` being a no-op.
- **Severity:** MEDIUM (security)
- **Recommended bounded task:** Verify DingTalk callback signatures/tokens, restrict to trusted caller IP or secret, expire OAuth states (store timestamp + TTL), and add security tests.

### M3. Role/menu permission management is a mock; no custom roles, buttons, or data scope
- **Requirements:** 2.6.2 (可自定义角色，勾选权限精确到菜单/按钮/数据范围；实时生效)
- **Evidence:** `frontend/src/views/system/RoleManage.vue` has a static role list + tree; `saveRoleMenus()` only shows a toast. No backend endpoints for roles/permissions. `frontend/src/utils/permission.js` hard-codes `ROLE_MENUS` (no button-level or data-scope support). `Sidebar.vue` does not render menus dynamically from permissions.
- **Severity:** MEDIUM-HIGH
- **Recommended bounded task:** Add role/permission tables and CRUD endpoints; return permission tree/button codes from `/user/info`; render sidebar/buttons from it; enforce data scope (本人/本部门/全部) in backend queries.

### M4. Stock alert DingTalk notification and restock request are not implemented
- **Requirements:** 2.4.2 (预警信息通过钉钉发送至库房管理员和采购员；补货申请提示采购员采购，含厂家联系方式、采购数量)
- **Evidence:** `StockAlertServiceImpl.scanAndAlert()` only inserts rows into `sys_stock_alert` and logs; no DingTalk send. `frontend/src/views/inventory/StockAlert.vue` `purchaseReq()` only shows a toast. Per-material threshold config exists as `minStock/maxStock` fields but there is no UI/endpoint to set them and no "已采购/已调拨" contact/quantity capture.
- **Severity:** MEDIUM
- **Recommended bounded task:** Add DingTalk robot/message sending on alert creation; add a restock-request record (material, quantity, supplier contact) and notify purchaser; add material threshold editing UI.

### M5. CIS component-library sync is entirely missing
- **Requirements:** 2.4.3 (同步物料编码/封装/值/库存/批次至CIS系统；支持手动全量/增量同步；实时)
- **Evidence:** No references to "CIS" or any sync code anywhere in `backend/src` or `frontend/src`.
- **Severity:** MEDIUM
- **Recommended bounded task:** Add a sync service/controller with full & incremental modes, a sync log table, and a frontend trigger page; decide whether "real-time" means event-driven or scheduled.

### M6. Stock flow is incomplete: no material/time/operation-type filters, no return records, in-memory pagination
- **Requirements:** 2.4.4 (按物料、时间、操作类型(入库/出库/退库)筛选；导出Excel)
- **Evidence:** `backend/.../controller/StockFlowController.java` only filters by `keyword`, loads all `in_record` + `tb_out_record` rows into memory and paginates in Java; there is no "return" type and no export endpoint. `frontend/src/views/inventory/StockFlow.vue` only has keyword search and a placeholder export.
- **Severity:** MEDIUM
- **Recommended bounded task:** Add DB-side paging/filters (materialId, time range, recordType including return) and an export endpoint; include return (退库) records.

### M7. Inbound/outbound record lists display wrong fields (material code column shows DB `materialId`)
- **Requirements:** 2.2.2 (入库记录含物料编码、物料名称、存放货位), 2.3.3 (出库记录关联出库单)
- **Evidence:** `frontend/src/views/inbound/InRecordList.vue` "物料编码" `prop="materialId"`, "物料名称" and "存放货位" always render `-` (InRecord entity has no such fields). `frontend/src/views/outbound/OutRecordList.vue` "物料编码" `prop="materialId"`. `InRecord`/`OutRecord` entities lack `materialCode`, `materialName`, `locationNo`.
- **Severity:** MEDIUM
- **Recommended bounded task:** Join material on query to return code/name/location; update entities/DTOs and mapper XML; fix column props.

### M8. Inbound batch audit not implemented
- **Requirements:** 2.2.1 (支持批量审核入库单)
- **Evidence:** Only single `confirm/{id}` endpoint exists; frontend confirms one row at a time.
- **Severity:** MEDIUM
- **Recommended bounded task:** Add a batch confirm endpoint (`POST /inbound/confirmBatch`) accepting a list of ids, with per-item stock validation and transaction.

### M9. Inbound/outbound statistics lack material, supplier, and department dimensions
- **Requirements:** 2.5.2 (某物料某时段入库总数/次数/平均批量；按供应商维度统计), 2.5.3 (按物料/时间/领料部门统计出库数量)
- **Evidence:** `StatisticsMapper.inboundStats/outboundStats` only group by time (`DATE_FORMAT`). No per-material, per-supplier, or per-department queries; no average batch qty.
- **Severity:** MEDIUM
- **Recommended bounded task:** Add SQL methods (or optional filter params) for material/supplier/dept dimensions; add bar-chart frontend for those.

### M10. Stagnant items report lacks last-out time and inventory amount
- **Requirements:** 2.5.4 (展示最后出库时间、当前库存数量、库存金额)
- **Evidence:** `StatisticsMapper.stagnantMaterials` returns only `lastOutDays` (days), not the actual last out time; no `库存金额` (material_cost × stock). `frontend/src/views/report/Stagnant.vue` shows "X天前".
- **Severity:** MEDIUM
- **Recommended bounded task:** Return last out timestamp and amount; display in the table; add export columns.

### M11. Data backup is incomplete (manual only; no schedule, incremental, retention)
- **Requirements:** 2.6.3 (每周日凌晨2点全量备份，每日增量备份，保留周期可配置)
- **Evidence:** `backend/.../controller/BackupController.java` only runs manual `mysqldump`. `frontend/src/views/system/DataBackup.vue` renders static strategy tags; `saveConfig()` only toasts. No scheduler for backup, no incremental backup, no retention cleanup. Also `mysqldump -p<password>` exposes the password in the process list.
- **Severity:** MEDIUM
- **Recommended bounded task:** Add backup scheduler (cron), incremental backup, retention cleanup, and configuration persistence; use `MYSQL_PWD` env or a config file to avoid password on CLI.

### M12. Batch user import/export missing; admin "add user" is broken (no password field)
- **Requirements:** 2.6.1 (支持批量导入/导出用户 Excel；删除用户时检查关联未完成单据)
- **Evidence:** `frontend/src/views/system/UserManage.vue` `handleImport/handleExport` toasts "待后端接口"; `handleSave` for a new user calls `userApi.register(form)` but the form has no `password`, so backend throws `"密码长度须为8~20位"` and add-user always fails. Backend `LoginController.delete` removes a user without checking associated incomplete orders.
- **Severity:** MEDIUM
- **Recommended bounded task:** Add admin create-user endpoint (sets initial password), batch import/export endpoints; add delete-user pre-check for open orders; wire frontend.

### M13. System log export and retention not implemented
- **Requirements:** 2.6.4 (支持按条件查询和导出；日志保留期限可设置，超期自动归档或删除)
- **Evidence:** `SysLog.vue` export toasts; `SysOperationLogServiceImpl` only inserts rows; no export endpoint, no retention setting/cleanup.
- **Severity:** MEDIUM
- **Recommended bounded task:** Add log export endpoint, configurable retention setting, and a scheduled archival/cleanup job.

### M14. Registration lacks server-side captcha, phone format, realName, and confirm-password validation
- **Requirements:** 2.1.1 (注册需填写验证码、真实姓名、手机号等；系统实时校验)
- **Evidence:** `UserServiceImpl.register` only checks username/phone existence and password complexity. No captcha verification, no phone pattern, no realName requirement. `LoginController.register` is public.
- **Severity:** MEDIUM
- **Recommended bounded task:** Add a register DTO with validation annotations, require captcha (reuse `CaptchaUtil`), validate phone format; add tests for invalid registrations.

### M15. CORS allows any origin with credentials
- **Requirements:** 2.1.2 (security posture)
- **Evidence:** `backend/.../config/CorsConfig.java`: `allowedOriginPatterns("*")` + `allowCredentials(true)`.
- **Severity:** MEDIUM (security)
- **Recommended bounded task:** Restrict `allowedOriginPatterns` to configured frontend origins; add tests for disallowed origins.

### M16. Hardcoded default JWT secret
- **Requirements:** 2.1.2 (authentication security)
- **Evidence:** `backend/.../util/JwtUtil.java`: `System.getenv().getOrDefault("JWT_SECRET", "local-evaluation-jwt-secret-change-me-123456789")`.
- **Severity:** MEDIUM (security)
- **Recommended bounded task:** Fail startup (or use a strong random secret) if `JWT_SECRET` is not provided outside local/dev; document rotation.

---

## LOW

### L1. Login rate limiter keyed only by username (not IP); failed-login attempts for unknown users are not aggregated
- **Evidence:** `backend/.../config/LoginRateLimiter.java`.
- **Severity:** LOW
- **Recommended bounded task:** Key rate limiting by IP+username and add a small delay on failures.

### L2. `MaterialSearch.vue` sends `packageType` filter as `warehouseCode`; backend has no package-type filter
- **Evidence:** `frontend/src/views/inventory/MaterialSearch.vue` `materialApi.page({..., warehouseCode: s.packageType})`; `MaterialMapper.xml` has no `package_type` filter.
- **Severity:** LOW
- **Recommended bounded task:** Add `packageType` param to material page query and pass it correctly.

### L3. "厂家批次" columns are hard-coded `-`
- **Evidence:** `MaterialSearch.vue`, `report/InventoryDetail.vue` render `-` for 厂家批次; the `Material` entity has no manufacturer/batch fields.
- **Severity:** LOW
- **Recommended bounded task:** Add manufacturer/batch fields to material (or derive from latest in/out record) and display them.

### L4. Stock alert scheduled scan runs at 2am, requirement says daily midnight; per-material threshold UI missing
- **Evidence:** `config/StockAlertScheduler.java` cron `0 0 2 * * ?`.
- **Severity:** LOW

### L5. Date-range queries use `BETWEEN` on `YYYY-MM-DD` strings, excluding the entire end date
- **Evidence:** `InRecordMapper.xml` `in_time BETWEEN #{start} AND #{end}`; `InRecordController.list/page`; frontend passes `YYYY-MM-DD`.
- **Severity:** LOW
- **Recommended bounded task:** Normalize end date to end-of-day (or use `<` on next day).

### L6. `OutRecordMapper.xml` defines `selectByDate` that is not declared in `OutRecordMapper.java` (dead SQL)
- **Evidence:** `backend/src/main/resources/mapper/OutRecordMapper.xml`.
- **Severity:** LOW

### L7. `StockFlowController` loads all records into memory (scalability)
- **Evidence:** `StockFlowController.page`.
- **Severity:** LOW (covered by M6).

### L8. `PurchaseInbound.vue` "手动新建入库单" button sets `showAddDialog=true` but no dialog is rendered
- **Evidence:** `frontend/src/views/inbound/PurchaseInbound.vue` line 7; `showAddDialog` never referenced in template.
- **Severity:** LOW-MEDIUM
- **Recommended bounded task:** Render the new-order dialog or remove the button.

### L9. `@element-plus/icons-vue` used directly but not declared in `package.json`
- **Evidence:** `frontend/package.json` vs `frontend/src/layout/Sidebar.vue`, `outbound/ProductionPicking.vue`, `report/ExportCenter.vue`; it only appears transitively in `package-lock.json`.
- **Severity:** LOW
- **Recommended bounded task:** Add `@element-plus/icons-vue` to `package.json` dependencies.

---

## Test Coverage & Project Health

- **T1. Missing tests (HIGH):** Only `backend/src/test/java/com/koolearn/bms/BmsApplicationTests.java` (contextLoads). There are no unit tests for `UserServiceImpl.register` (role injection), `MaterialServiceImpl` stock/lock concurrency, `InboundOrderServiceImpl.confirmIn`, `OutboundOrderServiceImpl` draft/submit/confirm/reject, `StockAlertServiceImpl.scanAndAlert`, statistics, or the interceptor/security layer. No frontend tests at all. No test DB config (H2 or Testcontainers) — the single `@SpringBootTest` requires a live MySQL at `localhost:3306/bms` (`backend/src/main/resources/application.properties`).
- **T2. No database schema/initialization (HIGH):** There is no `.sql` schema or Flyway/Liquibase migration in the repo. The application cannot start without a pre-provisioned MySQL `bms` database whose tables match all entities/XML (`tb_material`, `tb_inbound_order`, `in_storage_item`, `in_record`, `tb_outbound_order`, `tb_out_storage_item`, `tb_out_record`, `sys_user`, `sys_login_log`, `sys_operation_log`, `sys_stock_alert`). This makes verification of every feature dependent on an undocumented external setup.
- **T3. Mock/placeholder implementations inventory:** BOM import/match & 备料计划单 (H2), role management save (M3), restock request (M4), CIS sync (M5), export buttons in In/Out/StockFlow/SysLog/UserManage (H5), remember-password (H6), auto-backup strategy (M11), batch user import/export (M12), "读取钉钉采购单/退库单" buttons (fetchDingTalkOrders/fetchDingTalkReturns just re-call `load()`), `cleanExpiredStates()` no-op in `DingTalkLoginService`.

## Recommended remediation order
1. Fix C1 (role injection) and C2 (real-time permission) first — security.
2. Fix H3/H4 (broken order submission paths) and H1 (login redirect loop) — core usability.
3. Implement H5 (authenticated exports) and H2 (BOM backend).
4. Close M-series gaps (role management, alerts, CIS sync, backup, logs, stats).
5. Add schema/migrations and automated tests (T1/T2) covering the above.
