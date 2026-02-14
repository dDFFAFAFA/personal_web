# PHASE_TASK_BOARD_TEMPLATE

> 复制为 `docs/PHASE{N}_TASK_BOARD.md` 后使用。

## 1. 当前任务池

| ID | 任务 | Owner Thread | 状态 | 依赖 |
|----|------|--------------|------|------|
| P{N}-BEA-01 | 示例任务 | `BE-Thread-A` | TODO | 无 |
| P{N}-BEB-01 | 示例任务 | `BE-Thread-B` | TODO | 无 |
| P{N}-FE-01 | 示例任务 | `FE-Thread` | TODO | P{N}-BEA-01 |

状态值：
- `TODO`
- `IN_PROGRESS`
- `BLOCKED`
- `DONE`

## 2. 共享文件锁（后端双线程）

| 文件路径 | 锁定线程 | 关联任务ID | 状态 |
|----------|----------|------------|------|
| `backend/src/main/java/...` | - | - | FREE |

状态值：
- `FREE`
- `LOCKED`

## 3. Handoff 规则

所有线程交付都必须附带：

`[HANDOFF] branch=<branch> commits=<hash1,hash2> test="<cmd>:PASS/FAIL" risk="<text>"`
