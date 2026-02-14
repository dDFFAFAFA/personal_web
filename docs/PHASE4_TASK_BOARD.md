# PHASE4_TASK_BOARD

> 项目经理只维护本表并通知“任务已更新”，各线程自行执行。

## 1. 当前任务池

| ID | 任务 | Owner Thread | 状态 | 依赖 |
|----|------|--------------|------|------|
| P4-BEA-01 | `paper_summaries` 表 + V6 迁移 | `BE-Thread-A` | TODO | 无 |
| P4-BEA-02 | `POST /papers/{id}/summary/generate` | `BE-Thread-A` | TODO | P4-BEA-01 |
| P4-BEA-03 | `GET /papers/{id}/summary` + download | `BE-Thread-A` | TODO | P4-BEA-01 |
| P4-BEA-04 | 存储路径可移植治理（`file_storage_key`） | `BE-Thread-A` | TODO | P4-BEA-01 |
| P4-BEB-01 | `paper_repo_links` 表 + V6 迁移补充 | `BE-Thread-B` | TODO | 无 |
| P4-BEB-02 | `POST /papers/{id}/repo-links/extract` | `BE-Thread-B` | TODO | P4-BEB-01 |
| P4-BEB-03 | `GET /papers/{id}/repo-links` | `BE-Thread-B` | TODO | P4-BEB-01 |
| P4-BEB-04 | `POST /papers/{id}/repo-links/apply` + README 联动 | `BE-Thread-B` | TODO | P4-BEB-02 |
| P4-FE-01 | 首页概要区块（列表/状态） | `FE-Thread` | DONE | P4-BEA-03 |
| P4-FE-02 | 详情页概要生成与 markdown 下载 | `FE-Thread` | TODO | P4-BEA-03 |
| P4-FE-03 | 详情页链接提取与应用面板 | `FE-Thread` | TODO | P4-BEB-03 |

状态值：
- `TODO`
- `IN_PROGRESS`
- `BLOCKED`
- `DONE`

## 2. 共享文件锁（后端双线程使用）

| 文件路径 | 锁定线程 | 关联任务ID | 状态 |
|----------|----------|------------|------|
| `backend/src/main/java/com/changye/web/controller/PaperController.java` | - | - | FREE |
| `backend/src/main/java/com/changye/web/service/PaperService.java` | - | - | FREE |
| `backend/src/main/java/com/changye/web/model/Paper.java` | - | - | FREE |
| `backend/src/main/resources/db/migration/V6__phase4_summary_and_repo_links.sql` | - | - | FREE |

状态值：
- `FREE`
- `LOCKED`

## 3. Handoff 规则

所有线程交付都必须附带：

`[HANDOFF] branch=<branch> commits=<hash1,hash2> test="<cmd>:PASS/FAIL" risk="<text>"`
