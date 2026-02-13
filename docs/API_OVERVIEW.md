# API 概览文档

> **维护者**: Antigravity  
> **最后更新**: 2026-02-13  
> **后端地址**: `http://localhost:8080` (开发) | `https://your-domain.com` (生产)  
> **Base URL**: `/api/v1`

---

## 快速导航

| 模块 | 说明 | 状态 |
|------|------|------|
| [Health](#health-健康检查) | 健康检查 | ✅ Phase 0 已实现 |
| [Papers](#papers-论文管理) | 论文 CRUD | 📋 Phase 1 待开发 |
| [Notes](#notes-笔记管理) | 论文笔记 CRUD | 📋 Phase 1 待开发 |
| [Tags](#tags-标签管理) | 标签 CRUD | 📋 Phase 1 待开发 |

---

## 通用信息

### 统一响应格式

```json
{ "code": 200, "message": "success", "data": { ... }, "timestamp": "2026-02-13T20:00:00+08:00" }
```

### 分页响应

```json
{ "code": 200, "data": { "content": [...], "page": 0, "size": 20, "totalElements": 100, "totalPages": 5 } }
```

### 错误码

| code | 含义 |
|------|------|
| `200` | 成功 |
| `400` | 请求参数错误 |
| `404` | 资源不存在 |
| `409` | 资源冲突 |
| `500` | 服务器内部错误 |

---

## Health (健康检查)

### ✅ `GET /api/v1/health`

检查后端服务状态。

**curl**:
```bash
curl http://localhost:8080/api/v1/health
```

**响应**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "status": "UP",
    "version": "0.1.0",
    "timestamp": "2026-02-13T20:00:00+08:00"
  },
  "timestamp": "2026-02-13T20:00:00+08:00"
}
```

---

## Papers (论文管理)

> 📋 **Phase 1 — 待开发**

### `GET /api/v1/papers`

获取论文列表（分页 + 筛选）。

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `page` | int | 否 | 页码 (0-based, 默认 0) |
| `size` | int | 否 | 每页数量 (默认 20) |
| `sort` | string | 否 | 排序 (默认 `createdAt,desc`) |
| `status` | string | 否 | `UNREAD` / `SKIMMED` / `HALF_READ` / `FINISHED` / `NEED_REREAD` |
| `tagId` | long | 否 | 按标签筛选 |
| `starred` | boolean | 否 | 仅星标论文 |
| `keyword` | string | 否 | 搜索标题/作者/摘要 |

**响应示例**:
```json
{
  "code": 200,
  "data": {
    "content": [{
      "id": 1,
      "title": "Attention Is All You Need",
      "authors": ["Vaswani, A."],
      "year": 2017,
      "venue": "NeurIPS",
      "readingStatus": "FINISHED",
      "starred": true,
      "tags": [{ "id": 1, "name": "NLP", "color": "#409EFF" }],
      "noteCount": 3,
      "createdAt": "2026-02-10T10:00:00+08:00"
    }],
    "page": 0, "size": 20, "totalElements": 42, "totalPages": 3
  }
}
```

---

### `POST /api/v1/papers`

上传论文（PDF + 元数据）。

**Content-Type**: `multipart/form-data`

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `file` | File | ✅ | PDF (最大 50MB) |
| `title` | string | ✅ | 标题 |
| `authors` | string | 否 | JSON 数组, 如 `["Author A"]` |
| `year` | int | 否 | 年份 |
| `venue` | string | 否 | 会议/期刊 |
| `doi` | string | 否 | DOI |
| `abstractText` | string | 否 | 摘要 |
| `tagIds` | string | 否 | 标签 ID, 如 `[1,2]` |

**curl**:
```bash
curl -X POST http://localhost:8080/api/v1/papers \
  -F "file=@paper.pdf" \
  -F "title=My Paper" \
  -F "authors=[\"Author A\"]" \
  -F "year=2025"
```

---

### `GET /api/v1/papers/{id}`

获取论文详情（含笔记列表摘要）。

---

### `PUT /api/v1/papers/{id}`

更新论文元信息。

**Body**:
```json
{
  "title": "New Title",
  "authors": ["A", "B"],
  "year": 2025,
  "venue": "ICLR",
  "tagIds": [1, 3]
}
```

---

### `PATCH /api/v1/papers/{id}/status`

更新阅读状态。

**Body**: `{ "readingStatus": "HALF_READ" }`

**合法值**: `UNREAD` | `SKIMMED` | `HALF_READ` | `FINISHED` | `NEED_REREAD`

---

### `PATCH /api/v1/papers/{id}/star`

切换星标。

**Body**: `{ "starred": true }`

---

### `DELETE /api/v1/papers/{id}`

删除论文（含关联笔记和文件）。

---

### `GET /api/v1/papers/{id}/file`

下载/预览论文 PDF。返回 `application/pdf`。

---

## Notes (笔记管理)

> 📋 **Phase 1 — 待开发**

### `GET /api/v1/papers/{paperId}/notes`

获取某论文的全部笔记。

**响应**:
```json
{
  "code": 200,
  "data": [{
    "id": 1,
    "paperId": 1,
    "title": "核心思想",
    "content": "## Self-Attention\n\n...",
    "sortOrder": 1,
    "createdAt": "...",
    "updatedAt": "..."
  }]
}
```

---

### `POST /api/v1/papers/{paperId}/notes`

新增笔记。

**Body**: `{ "title": "笔记标题", "content": "## Markdown 内容" }`

---

### `PUT /api/v1/notes/{noteId}`

更新笔记。

**Body**: `{ "title": "新标题", "content": "新内容", "sortOrder": 1 }`

---

### `DELETE /api/v1/notes/{noteId}`

删除笔记。

---

## Tags (标签管理)

> 📋 **Phase 1 — 待开发**

### `GET /api/v1/tags`

获取全部标签。

**响应**:
```json
{
  "code": 200,
  "data": [
    { "id": 1, "name": "NLP", "color": "#409EFF", "paperCount": 12 },
    { "id": 2, "name": "Security", "color": "#E6A23C", "paperCount": 5 }
  ]
}
```

---

### `POST /api/v1/tags`

新增标签。`409` if 重名。

**Body**: `{ "name": "Deep Learning", "color": "#F56C6C" }`

---

### `PUT /api/v1/tags/{id}`

更新标签。

**Body**: `{ "name": "DL", "color": "#909399" }`

---

### `DELETE /api/v1/tags/{id}`

删除标签（不删关联论文，仅解除关系）。

---

## 阅读状态说明

| 值 | 中文 | 说明 |
|----|------|------|
| `UNREAD` | 未读 | 默认 |
| `SKIMMED` | 读了一点 | 浏览了摘要/引言 |
| `HALF_READ` | 读了一半 | 读到核心部分 |
| `FINISHED` | 精读完成 | 完整阅读 |
| `NEED_REREAD` | 需要重读 | 需要再次精读 |

---

> 📖 完整请求/响应定义详见 [API_CONTRACT.md](./API_CONTRACT.md)
