# Coding Agent Harness Evaluation on a Real-World Project

> 用同一个模型、同一份未完成项目和同一套需求，对比 mini-swe-agent、DeepSeek Harness 与 Pi 在真实全栈开发任务中的表现。

[English summary](#english-summary) · [验收报告](docs/acceptance-report.md) · [评测方法](docs/methodology.md) · [复现说明](evaluation/README.md)

## 为什么做这个项目

我发起这个实验，是因为不想只看排行榜或演示视频，而是想亲自理解一个 coding-agent harness 到底怎样读取需求、调查代码、调用工具、组织子 Agent、修改项目并验证结果。

我选择了一套自己有权使用、但离开原工作时尚未完成的物料管理系统作为真实任务。它包含 Vue 前端、Spring Boot 后端和一份较完整的业务需求，既有残缺功能，也有权限、库存并发、报表、备份和外部系统集成等现实问题。

实验有两个问题：

1. 相同模型放进不同 harness，会产生多大差异？
2. Agent 自己声称“完成”之后，独立验收还能发现什么？

## 被测对象

| Harness | 固定版本 | 使用方式 |
|---|---|---|
| [mini-swe-agent](https://github.com/SWE-agent/mini-swe-agent) | 2.4.6 | 单 Agent 自主实现 |
| [DeepSeek Harness](https://github.com/deepseek-ai/deepseek-harness) | commit `47f943859bef60e4160492346772ded9b24f765a` | 原生 headless，启用官方工作流与子 Agent |
| [Pi](https://github.com/earendil-works/pi) | 0.84.2 / commit `914cf1472e715297caa30db4b9535d534a9eb718` | 官方 subagent 示例，仅将子 Agent 模型适配为 DeepSeek |

三者均使用 `deepseek/deepseek-v4-flash`。网络访问没有人为禁止；钉钉和 CIS 没有真实企业测试环境，因此允许可配置适配器或确定性 mock。三次运行按顺序进行，避免本地电脑资源竞争。

## 结果

| 排名 | Harness | 总分 | 外部验收测试 | 自带后端测试 | 主要结论 |
|---:|---|---:|---:|---:|---|
| 1 | DeepSeek Harness | **88** | 12 / 13 | 32 | 功能覆盖和库存核心正确性最好 |
| 2 | Pi | **86** | 12 / 13 | 40 | 测试工程最好，但接受负数出库 |
| 3 | mini-swe-agent | **62** | 9 / 13 | 10 | 改动广泛，但关键缺陷和页面问题较多 |

评分由需求完成度 45、正确性与安全 25、构建与测试 15、真实页面可用性 10、过程与可复现性 5 组成。完整依据见[验收报告](docs/acceptance-report.md)和[评分表](evaluation/scorecard.csv)。

## 最值得关注的发现

- 三个候选都能完成后端和前端生产构建，但三个都没有前端自动化测试。
- mini-swe-agent 在注册验证码、确认密码、负数出库和并发库存占用上失败，并且根路径会触发路由无限重定向白屏。
- DeepSeek Harness 的功能表面最完整，尤其是按钮权限、数据范围、CIS 和物料管理，但后端没有校验注册确认密码。
- Pi 自带测试最多、完成说明也最准确，但仍接受负数出库；其钉钉登录按钮还存在前端参数接线错误。
- Agent 的“最终总结”不能代替独立验收。DeepSeek Harness 明确声称确认密码已完成，但外部测试证明并非如此。
- Harness 的过程质量也有差异：DeepSeek Harness 运行中破坏了原 worktree 边界；Pi 生成了约 788 MiB 的轨迹文件。

## 实验流程

```text
脱敏基线 + 需求
        │
        ├── mini-swe-agent ──→ 冻结结果
        ├── DeepSeek Harness ─→ 冻结结果
        └── Pi + subagents ───→ 冻结结果
                                  │
                                  ├── 干净构建与自带测试
                                  ├── API 合同与源码检查
                                  ├── 13 个外部验收测试
                                  ├── 顺序启动与真实页面抽查
                                  └── 统一评分与过程审计
```

## 仓库导航

- [`requirements/`](requirements/)：净化后的 [Word 原始需求](requirements/material-management-requirements.docx)与 [Markdown 阅读版](requirements/requirements.md)。
- [`baseline/`](baseline/)：统一起点。
- [`harnesses/`](harnesses/)：固定版本、任务文本和实际配置。
- [`candidates/`](candidates/)：三个冻结后的完整源码快照。
- [`evaluation/`](evaluation/)：外部测试、脚本、评分和证据摘要。
- [`results/`](results/)：候选补丁、提交列表与 Agent 自述完成情况。
- [`docs/`](docs/)：背景、方法、环境、踩坑、验收和脱敏说明。

仓库提供一个手动触发的 GitHub Actions 工作流，用于顺序验证三个候选的后端测试和前端构建。它默认不会在每次推送时自动运行，避免 Private 阶段无意消耗 Actions 配额。

## 如何理解这个排名

这不是 SWE-bench，也不是对三个开源项目普遍能力的最终结论。排名只适用于这里固定的：

- 项目与需求；
- 模型与时间点；
- harness 版本和原生配置；
- 本地环境与评测规则。

本实验刻意保留各 harness 的原生设计哲学，因此不是严格等 token、等步骤的模型能力测试，更接近一次“真实使用效果”比较。

## 数据与许可说明

仓库内容已通用化并移除 API Key、缓存、构建产物、数据库备份和完整轨迹。需求 Word 原件的组织/人员标识、文档元数据、识别性截图和进度日期栏也已移除；候选源码的展示名称被替换，但没有进行功能修复。详见[脱敏说明](docs/sanitization.md)。

当前仓库暂不附加开源许可证。没有许可证不代表可以任意复制、修改或再发布代码。上游 harness 仅通过链接和版本标识引用，不在本仓库重新分发。

## English summary

This repository documents a hands-on comparison of three coding-agent harnesses—mini-swe-agent, DeepSeek Harness, and Pi—on the same unfinished Vue + Spring Boot material-management system. All runs used the same DeepSeek model, while each harness retained its native workflow and subagent philosophy. Results were frozen and independently evaluated through clean builds, API-contract checks, 13 external acceptance tests, runtime UI inspection, and process auditing. DeepSeek Harness ranked first at 88/100, Pi followed at 86/100, and mini-swe-agent scored 62/100. The goal is not to claim a universal benchmark winner, but to provide a transparent real-world case study of how harness architecture changes outcomes.
