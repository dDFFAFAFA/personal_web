# Repo Sync Error Codes (P4-REPO-SUBSYNC-02)

## Scope
- Endpoint: `POST /api/v1/repo/sync`
- Status endpoint: `GET /api/v1/repo/sync-status`

## Contract

| HTTP / code | Trigger | Stable message rule |
|---|---|---|
| `400` | 配置参数不完整、私钥缺失、目标目录不可用、仓库地址非法 | 返回可读中文消息，不暴露密钥内容 |
| `404` | `repo_config(id=1)` 不存在（未初始化仓库配置） | `仓库配置不存在，请先保存配置` |
| `500` | git 执行失败 / 外部仓库不可达 / 超时 | `同步失败: <trimmed output>` 或 `执行 git 命令失败` |

## Persistence (Backward Compatible)

新增字段（可空）：
- `repo_config.last_sync_error_code` (`INTEGER`)
- `repo_config.last_sync_duration_ms` (`BIGINT`)

迁移脚本：`backend/src/main/resources/db/migration/V8__add_repo_sync_result_metadata.sql`

兼容策略：
- 旧数据字段为空时，`GET /api/v1/repo/sync-status` 仍返回 `200`，并允许 `errorCode/durationMs` 为 `null`。
- 新字段只追加，不影响既有读写路径。

## Ownership / Collaboration
- 文档说明由协同任务 `P4-REPO-SUBSYNC-02` 更新。
- 功能变更仅触达后端 repo-sync 链路，不改动前端接口路径。

## Rollback

1. 应用回滚（保留数据库新增列）：
- 直接回退到旧后端版本可运行；新增列是可选字段，不破坏旧代码。

2. 数据库回滚（如必须撤销 schema）：
```sql
ALTER TABLE repo_config DROP COLUMN IF EXISTS last_sync_error_code;
ALTER TABLE repo_config DROP COLUMN IF EXISTS last_sync_duration_ms;
```

3. 风险提示：
- 删除列后，新版本 `sync-status` 中的 `errorCode/durationMs` 将无法返回。
- 建议优先应用级回滚，避免非必要 DDL 变更。
