# 复现评测

## 快速检查

每个候选都包含独立的 `backend/` 与 `frontend/`：

```bash
cd candidates/<mini|dsh|pi>/backend
mvn clean test

cd ../frontend
npm ci
npm run build
```

建议分别设置独立 Maven 缓存并顺序执行，避免三套构建同时占用内存与磁盘。

## 外部验收测试

`evaluation/tests/` 中的两个 Java 测试文件在 Agent 运行时不可见：

- `ExternalBehaviorAcceptanceTest.java`
- `ExternalInventoryAcceptanceTest.java`

`evaluation/scripts/original/` 保存本次实验实际使用的脚本。它们记录了原始 D 盘路径，作为过程证据保留；在其他机器上使用前，需要把根路径变量改为当前仓库位置。

## 证据

- `scorecard.csv`：机器可读分数。
- `evidence/mechanical-summary.txt`：构建与自带测试汇总。
- `evidence/api-contract-check.txt`：前后端字面 API 合同检查。
- `evidence/hidden-test-summaries/`：外部测试结果摘要。
- `docs/acceptance-report.md`：完整判定与运行态观察。

## 重要说明

首次公开后，这些测试不再是隐藏测试。如果使用本仓库重新运行 Agent，必须另写新的保留测试，才能继续评估未知缺陷发现能力。

