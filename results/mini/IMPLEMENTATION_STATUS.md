# Implementation Status

Last updated: 2026-08-15

## Completed items

### Security (CRITICAL)
- **C1. Registration privilege escalation** — `UserServiceImpl.register` always assigns `engineer`; server-side DTO excludes `role`. Covered by `RegistrationRoleRegressionTest`.
- **C2. Real-time permission enforcement** — `LoginInterceptor` re-queries the user's current role/status from the database on every request; disabled users are rejected immediately and role changes take effect without re-login. Covered by `SecurityFlowTest`.
- **M1. Missing `@RequireRole`** — inbound/outbound save/submit/update/reject methods now annotated with appropriate roles.
- **M2. DingTalk callback authentication** — added `DingTalkCallbackVerifier` (HMAC-SHA256 signature); OAuth `state` now has 10-minute expiry with cleanup.
- **M14. Registration validation** — `UserRegisterDTO` has `@NotBlank`, `@Size`, `@Pattern` for phone; controller uses `@Valid`.
- **M15. CORS** — restricted to configured origins (`bms.cors.allowed-origins`), no longer `*` with credentials.
- **M16. JWT secret** — `JwtUtil` is now a Spring bean reading `bms.jwt.secret`; strict mode fails startup if default secret is used; length >= 32 bytes enforced.

### Login / Frontend (HIGH)
- **H1. Login redirect loop** — login redirects to first role-appropriate menu (`getFirstAllowedMenu`); router guard no longer loops on forbidden paths; catch-all route added.
- **L1. Rate limiter** — login rate limit keyed by `username|IP`.

### Core Flows
- **H3. Outbound submission** — `OutboundOrderServiceImpl.saveOrder` now creates a draft + items (and locks stock) when no `id` is provided; frontend `ProductionPicking.vue` wired accordingly.
- **Inbound confirm** — `InboundOrderServiceImpl.confirmIn` now auto-matches or creates materials for manually created inbound orders, writes `in_record`, and auto-generates material codes.
- **M8. Batch inbound confirm** — added `POST /inbound/confirmBatch`.
- **M7. Record fields** — `in_record`/`tb_out_record` queries LEFT JOIN `tb_material` to return `materialCode`, `materialName`, `locationNo`; frontend columns updated.

### Inventory / Reports / Exports
- **L2.** Material `packageType` filter added to backend and frontend.
- **L4.** Stock alert scan moved to daily midnight.
- **L5.** Date-range queries now inclusive of end date (end-of-day).
- **M6. Stock flow** — filters (keyword/materialId/recordType/time range) and authenticated Excel export added.
- **M9/M10. Statistics** — inbound by material/supplier, outbound by material/dept; stagnant report now includes last-out time and inventory amount.
- **H5/Exports** — authenticated Excel exports for inbound records, outbound records, stock flow, material inventory, statistics, system logs, and user lists; frontend uses `downloadFile` with the JWT header.

### System Management
- **M11. Backup** — `BackupService` uses `MYSQL_PWD` (no password on CLI), records history, supports scheduled full/incremental backups, retention cleanup, and strategy configuration endpoints.
- **M12. User management** — admin create-user endpoint (with initial password), batch import/export via Excel, delete-user pre-check for uncompleted orders.
- **M13. System logs** — export endpoint, configurable retention (`sys_config`), scheduled cleanup.
- **M3. Roles/permissions** — `sys_role`/`sys_role_menu` backend CRUD endpoints; frontend `RoleManage.vue` loads/saves menu permissions.

### Alerts / CIS / BOM / DingTalk
- **M4. Stock alert** — DingTalk robot notifier (demo mode logs), restock request records (`sys_restock_request`) + create/handle endpoints; frontend prompts for qty/contact.
- **M5. CIS sync** — `CisSyncService` with `mock`/`http` adapter modes, sync log table, manual trigger endpoint.
- **H2. BOM** — backend BOM import/match/save-plan endpoints with versioning and match history tables; frontend `saveAsPlan` now persists a plan.
- **DingTalk approval** — demo mode returns deterministic fake instance IDs instead of calling external APIs.

### Build & Schema (T1/T2)
- **T2. Database schema** — `backend/src/main/resources/schema.sql` (idempotent, MySQL + H2 MySQL-mode compatible); default admin seeded via `data.sql` (password `admin123`).
- **T1. Tests** — `BmsApplicationTests` (H2 context), `RegistrationRoleRegressionTest`, `SecurityFlowTest`, `InboundOutboundFlowTest` all use H2 in-memory DB.

## Test commands & actual results

```bash
cd backend && mvn test
```
- `BmsApplicationTests.contextLoads` — PASS
- `RegistrationRoleRegressionTest` (2 tests) — PASS
- `SecurityFlowTest` (4 tests) — PASS
- `InboundOutboundFlowTest` (3 tests) — PASS
- **Total: 10 tests, 0 failures, 0 errors**

```bash
cd frontend && npm install && npm run build
```
- `npm install` — PASS (100 packages)
- `npm run build` — PASS (dist generated)

```bash
cd backend && mvn -DskipTests package
```
- `mvn package -DskipTests` — PASS (executable jar produced)

## Remaining items / blockers

1. **Real DingTalk live integration** — external service unavailable. Configured adapters work in **demo mode** (deterministic local results, no real network calls). Set `DINGTALK_APP_KEY/SECRET`, `DINGTALK_ROBOT_WEBHOOK`, and `DINGTALK_CALLBACK_SECRET` for live use.
2. **Real CIS live integration** — `CIS_ADAPTER_MODE=http` posts to configured base URL, but no live CIS server available for verification; default `mock` mode used.
3. **MySQL incremental backup** — true binlog-based incremental backup requires server setup; current `incremental` mode degrades to a logical full backup with `--single-transaction`.
4. **Frontend automated tests** — no frontend test framework (Vitest) configured yet; only production build verified.
5. **BOM client-side matching** — import tab still shows a client-side preview; authoritative match persists via backend `/outbound/bom/import` and `/savePlan`.
6. **Login captcha for registration** — registration dialog does not yet collect a captcha; server-side validation covers phone/realName/password.
7. **Button-level/data-scope permissions** — role menu permissions are persisted and enforced at menu level; fine-grained button codes / data scope enforcement in backend queries remains partial.
8. **DingTalk order fetching** — "读取钉钉采购单/退库单" buttons currently re-load local data (no live DingTalk integration available).

## Notes

- `schema.sql` is the single source of truth for the database schema.
- All external-service integrations are behind configurable adapters with deterministic mock/demo modes; no claim of live integration is made.
- The default admin account is `admin` / `admin123` (must be changed on first login).
