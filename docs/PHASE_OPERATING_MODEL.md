# PHASE_OPERATING_MODEL

> 目标：定义“产品经理 + 多线程开发 + reviewer 合并”的固定循环。  
> 适用于 Phase4 及后续所有 Phase。

## 1. 组织与角色

1. `PM-Lead`（你）
- 定义产品目标与验收结果
- 启动验收（UAT）并给出 go/no-go

2. `PM-Architect`（我 / Codex）
- 输出设计文档、约束、任务看板
- 维护协作规则与质量门禁

3. `BE-Thread-A`
- 后端子域 A 功能开发

4. `BE-Thread-B`
- 后端子域 B 功能开发

5. `FE-Thread`
- 前端交互与页面开发

6. `REVIEW-Thread`
- 代码审查、冲突处理、阶段集成、回归验证

## 2. 分支模型（每个 Phase 重复）

以 `PhaseN` 为例：

1. 阶段集成分支（reviewer 专用）
- `codex/p{N}-integration`

2. 线程基线分支
- `codex/p{N}-be-a`
- `codex/p{N}-be-b`
- `codex/p{N}-fe`
- `codex/p{N}-review`

3. 功能分支（开发线程必须从线程基线切出）
- `codex/p{N}-be-a-{feature}`
- `codex/p{N}-be-b-{feature}`
- `codex/p{N}-fe-{feature}`

## 3. 固定工作流（强制）

1. `PM-Lead + PM-Architect` 更新：
- `docs/PHASE{N}_GOALS.md`
- `docs/PHASE{N}_API_DB_DRAFT.md`
- `docs/PHASE{N}_TASK_BOARD.md`

2. 开发线程执行：
- 先拉功能分支
- 完成后先合回自己的线程基线分支
- 提交 `HANDOFF`

3. `REVIEW-Thread` 执行：
- 从各线程基线分支收提交
- 合并到 `codex/p{N}-integration`
- 复跑质量门禁并输出审查结论

4. `PM-Lead` 启动验收：
- 在阶段环境验证关键流程（UAT）
- 验收结果为 `PASS` 才允许发布

5. 发布合并：
- `codex/p{N}-integration` -> `main`
- 打 Tag（可选）
- 关闭当前 Phase，进入下一 Phase

## 4. 质量门禁

1. 后端必须：`mvn -q test`
2. 前端必须：`npm run build`
3. reviewer 在 integration 分支至少复跑一次后端+前端门禁
4. 未通过门禁禁止进入 UAT

## 5. 管理约定

1. 项目经理不再逐线程写临时提示词。
2. 只更新任务看板并通知“任务已更新”。
3. 各线程必须自行读取 `CONTRACT.md`、`OWNERSHIP.md`、`AGENT_RUNBOOK.md`、阶段任务看板。
