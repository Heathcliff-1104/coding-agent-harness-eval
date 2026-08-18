# GitHub 发布规划：三种代码 Agent 的真实项目对比实验

## 一、项目定位

这不是单纯上传三个“改完后的代码目录”，而是发布一个可解释、可复查、可复现的完整评测项目：

> 以一套本人有权使用、但尚未开发完成的物料管理系统为统一基线，在相同需求、相同模型与尽可能一致的环境下，分别使用 mini-swe-agent、DeepSeek Harness 和 Pi 完成代码审查与自主实现，再通过独立构建、隐藏测试、真实页面验收和过程审计比较三种 Agent 架构的实际效果。

项目重点不只是“谁分数高”，还要展示：

- 三种 harness 的设计哲学如何影响执行过程。
- 同一个模型在不同 Agent 架构下会产生什么不同结果。
- 如何设计相对公平、可复查的个人代码 Agent 评测。
- Agent 自述“已完成”与独立验收结果之间可能存在的差距。
- 普通开发者从零搭建这些工具时会遇到哪些真实问题。

## 二、推荐仓库形式

推荐新建一个独立仓库，不要把当前 `D:\wuliao\agent-eval` 本身直接设为远程仓库。

建议仓库名：

1. `coding-agent-harness-eval`（推荐，清楚、专业）
2. `deepseek-agent-harness-comparison`
3. `real-world-coding-agent-eval`
4. `material-system-agent-benchmark`

推荐先设为 **Private**。完成最终脱敏、许可证和 README 审阅后，再一键改为 Public。

## 三、发布版目录结构

```text
coding-agent-harness-eval/
├─ README.md                         # 初衷、实验问题、主要结论、快速导航
├─ LICENSE                           # 由作者明确选择；未决定前不添加
├─ NOTICE.md                         # 上游 harness、模型与第三方项目说明
├─ SECURITY.md                       # 说明所有凭据均为演示值，禁止提交真实密钥
├─ .gitignore
├─ docs/
│  ├─ background.md                 # 项目背景与个人初衷
│  ├─ methodology.md                # 公平性原则、实验变量、局限性
│  ├─ environment.md                # WSL、Java、Node、Maven、模型与版本
│  ├─ rubric.md                     # 100 分评分规则
│  ├─ acceptance-report.md          # 正式验收报告
│  ├─ lessons-learned.md            # 安装、代理、D 盘、Git/worktree 等踩坑
│  └─ github-publication-audit.md    # 发布前脱敏说明
├─ requirements/
│  ├─ requirements.md               # 脱敏后的原始需求
│  └─ assets/                        # 仅保留展示所需图片；优先转换为 PNG
├─ baseline/
│  ├─ frontend/
│  ├─ backend/
│  └─ BASELINE.md                    # 基线提交、脱敏说明、已知残缺项
├─ harnesses/
│  ├─ mini-swe-agent/
│  │  ├─ README.md                   # 上游链接、固定版本、运行方式
│  │  ├─ task.txt
│  │  └─ config/                     # 公开配置，不含 API Key
│  ├─ deepseek-harness/
│  │  ├─ README.md
│  │  ├─ task.txt
│  │  └─ config/                     # 原生 profile patch、并发参数
│  └─ pi/
│     ├─ README.md
│     ├─ task.txt
│     ├─ agent-model-adaptation.diff # 官方 subagent 仅模型字段适配的证据
│     └─ config/
├─ candidates/
│  ├─ mini/                          # 冻结后的完整源码快照，不含 .git/构建物
│  ├─ dsh/
│  └─ pi/
├─ evaluation/
│  ├─ README.md                      # 如何重跑验收
│  ├─ tests/
│  │  ├─ ExternalBehaviorAcceptanceTest.java
│  │  └─ ExternalInventoryAcceptanceTest.java
│  ├─ scripts/                       # 构建、静态检查、API 合同、隐藏测试脚本
│  ├─ scorecard.csv
│  └─ evidence/
│     ├─ mechanical-summary.txt
│     ├─ api-contract-check.txt
│     ├─ hidden-test-summaries/
│     ├─ source-identities.txt
│     └─ runtime-ui-notes.md
├─ results/
│  ├─ mini/
│  │  ├─ IMPLEMENTATION_STATUS.md
│  │  ├─ commits.txt
│  │  └─ changes.patch
│  ├─ dsh/
│  └─ pi/
└─ .github/
   └─ workflows/
      └─ verify.yml                  # 可选：自动构建和运行公开测试
```

## 四、哪些内容保留

### 必须保留

- 脱敏需求和原始基线源码。
- 三个冻结候选源码快照。
- 每个 harness 的上游链接、固定版本/提交、实际任务文本和公开配置。
- 模型名称、并发数、步数/时间/费用限制等实验变量。
- 构建脚本、隐藏测试、API 合同检查和评分规则。
- 正式验收报告、评分表、测试摘要和已知限制。
- 三个候选的 `changes.patch`、提交列表和 `IMPLEMENTATION_STATUS.md`。
- 对 DSH 破坏 worktree 边界、Pi 轨迹膨胀等过程问题的如实记录。

### 可选保留

- 精简后的关键轨迹片段，例如每个 Agent 的决策摘要、工具调用统计和关键转折。
- 页面截图，前提是截图中没有真实人员、公司、内网地址或有效账号。
- Git bundle。由于它与完整候选源码重复且不方便浏览，第一版不建议上传。

## 五、明确排除

- `DEEPSEEK_API_KEY`、GitHub Token、`.env`、私钥、浏览器登录状态。
- `cache/`、Maven repository、npm cache。
- 所有 `node_modules/`、`target/`、`dist/`、运行日志和临时数据库。
- Pi 的 `trajectory.jsonl`（约 788 MiB）。
- 三个完整 harness 上游源码副本。仓库只记录上游 URL 和固定提交；避免重复分发和体积膨胀。
- 当前 `verification/` 的四套构建副本。
- 当前 `worktrees/`、`workspaces/` 中的 `.git` 目录和嵌套仓库元数据。
- API 返回的真实数据、数据库备份、失效与否尚未确认的真实账号或联系方式。

## 六、为什么不能直接上传当前目录

当前目录至少包含：

- `results/` 约 802 MiB，其中 Pi 单个轨迹约 788 MiB。
- `cache/` 约 173 MiB。
- `verification/` 内重复的 `node_modules`、Maven `target` 和四套源码/构建副本。
- baseline、worktree、standalone clone 等多套嵌套 Git 边界。

GitHub 对普通 Git 文件超过 50 MiB会警告，超过 100 MiB会阻止推送。即使使用 Git LFS，原始轨迹对读者也几乎没有价值，会让仓库难以克隆。发布版应保留“完整实验信息”，而不是保留“完整运行缓存”。

## 七、发布前安全审查

发布目录生成后，至少执行以下四层检查：

1. 文件名检查：`.env`、私钥、数据库、备份、日志、凭据文件。
2. 内容检查：DeepSeek/GitHub Token 格式、私钥头、密码和真实接口地址。
3. Git 暂存区检查：确认 `.gitignore` 生效，检查将要提交的每一个文件。
4. Git 历史检查：首次推送前扫描整个新仓库历史；发现密钥时直接重建首次提交，不做“删除后继续推送”。

GitHub Push Protection 是最后一道保护，不应替代本地审查。

## 八、README 推荐叙事

README 首页建议按以下顺序：

1. 一句话项目介绍。
2. “为什么做这个实验”：希望通过亲自运行与验收真实理解 harness，而不是只看榜单。
3. 被测对象与控制变量。
4. 实验流程图：基线 → 三个独立运行 → 冻结 → 构建/隐藏测试/页面验收 → 评分。
5. 主要结果表：DSH 88、Pi 86、mini 62。
6. 最有价值的发现，而不是只宣布冠军。
7. 如何复现实验。
8. 仓库目录导航。
9. 局限性、许可证和免责声明。

建议明确写出：这是一次个人真实项目实验，不是 SWE-bench，也不是对各项目普遍能力的最终排名；结果只适用于固定版本、固定模型、固定任务和该代码库。

## 九、建议提交历史

不要把所有内容塞进一个巨型提交。推荐：

1. `chore: initialize sanitized evaluation repository`
2. `docs: add motivation requirements and methodology`
3. `feat: add frozen baseline and candidate snapshots`
4. `test: add independent acceptance suite and runners`
5. `docs: publish scorecard evidence and final report`
6. `ci: add reproducibility workflow`（若启用 GitHub Actions）

这样读者能看懂项目是如何形成的，也便于审查每一层材料。

## 十、真正上传时需要作者提供的信息

不需要密码，也不要在聊天中发送 Token。需要：

1. GitHub 用户名，或目标 Organization 名称。
2. 仓库名称。
3. 首次创建为 Private 还是 Public；推荐 Private。
4. README 使用中文，还是中英双语；推荐中文主文 + 英文摘要。
5. 公开署名：GitHub 用户名、真实姓名或昵称。
6. 许可证选择。若尚未确认公司代码的再许可范围，推荐第一版暂不添加开源许可证并保持 Private；确认后再选择 MIT、Apache-2.0 或其他许可证。

登录方式推荐在本机运行 GitHub CLI 的浏览器授权 `gh auth login`，由作者亲自在浏览器确认。授权完成后再创建仓库和推送，不需要向任何人复制凭据。

## 十一、执行顺序

1. 作者确认上述六项信息。
2. 在 D 盘创建新的发布暂存目录，例如 `D:\wuliao\coding-agent-harness-eval`。
3. 自动复制白名单内容，而不是从原目录删除黑名单内容。
4. 生成 README、方法文档、三个 harness 说明和复现指引。
5. 对新目录执行敏感信息、大文件、嵌套 Git 和许可证审查。
6. 本地初始化 Git，并分层提交。
7. 作者通过 GitHub CLI 浏览器登录。
8. 创建 Private 仓库并推送。
9. 在 GitHub 页面进行最终人工预览。
10. 若决定公开，再补许可证、公开免责声明并切换 Public。
