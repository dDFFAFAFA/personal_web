# AGENT_RUNBOOK (Phase4)

> 目标：让各线程“看文档即执行”，不依赖临时提示词。

## 1. 启动检查（每次开工前）

1. 确认工作目录是自己的 worktree。
2. 确认当前分支是自己的线程分支或功能分支。
3. 阅读并遵守：
- `CONTRACT.md`
- `OWNERSHIP.md`
- `docs/PHASE4_TASK_BOARD.md`

## 2. 线程职责

1. `BE-Thread-A`
- 摘要生成链路（PDF -> LLM -> markdown）
- 概要查询/下载 API
- 存储迁移与路径可移植治理

2. `BE-Thread-B`
- PDF 中 GitHub/Gitee 链接提取
- 候选链接管理与 apply 到 `paper_code_entries`
- README 重建联动

3. `FE-Thread`
- 首页概要展示
- 论文详情：生成概要/下载 md/链接提取与应用
- 完整 loading/error/empty 反馈

4. `REVIEW-Thread`
- 审查各线程提交
- `cherry-pick` 到 `codex/p4-integration`
- 复跑门禁并输出合并结论

## 3. 分支规则

1. 线程基线分支：
- `codex/p4-be-a`
- `codex/p4-be-b`
- `codex/p4-fe`
- `codex/p4-review`
- `codex/p4-integration`（仅审查线程可写）

2. 功能分支命名：
- `codex/p4-be-a-{feature}`
- `codex/p4-be-b-{feature}`
- `codex/p4-fe-{feature}`

3. 禁止直接提交：
- `main`
- `develop`
- 他人线程分支

## 4. 文件锁规则（后端双线程强制）

1. 改共享后端文件前，先在 `docs/PHASE4_TASK_BOARD.md` 登记“文件锁”。
2. 未登记文件锁，禁止修改共享文件。
3. 完成后必须释放文件锁（状态改为 `DONE` 或 `FREE`）。

## 5. 质量门禁

1. 后端：`mvn -q test`
2. 前端：`npm run build`
3. 审查线程在 integration 至少复跑一次后端门禁和前端门禁。

## 6. 交付格式（必须）

`[HANDOFF] branch=<branch> commits=<hash1,hash2> test="<cmd>:PASS/FAIL" risk="<text>"`
