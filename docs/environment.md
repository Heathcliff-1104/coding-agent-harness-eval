# 实验环境

## 主机与运行环境

| 项目 | 版本 |
|---|---|
| Host | Windows 11, build 26100 系列 |
| WSL | Ubuntu 26.04 LTS / WSL2 |
| Linux kernel | 6.18.33.2-microsoft-standard-WSL2 |
| Node.js | 22.22.1 |
| npm | 9.2.0 / 10.9.4（不同安装阶段） |
| pnpm | 11.7.0（DeepSeek Harness） |
| Java | OpenJDK 17.0.19 |
| Maven | 3.9.12 |

代码、缓存和主要评测结果位于 D 盘。Pi 的可执行包最终放在 WSL Linux 文件系统中，以避免从 `/mnt/d` 加载大量 Node 模块时出现极慢启动。

## Harness 身份

| Harness | 身份 |
|---|---|
| mini-swe-agent | 2.4.6 |
| DeepSeek Harness | `47f943859bef60e4160492346772ded9b24f765a` |
| Pi source | `914cf1472e715297caa30db4b9535d534a9eb718` / tag `v0.84.2` |

## 模型

所有候选使用 `deepseek/deepseek-v4-flash`。API Key 只通过本机环境文件加载，没有写入评测源码或发布仓库。

## 测试数据库

候选自带测试与外部验收使用 H2 的 MySQL 兼容模式。真实 MySQL 8 回归属于后续工作，因此报告中不会把 H2 通过等同于生产数据库通过。

