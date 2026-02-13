# API 接口契约 (API_CONTRACT.md)

> **版本**: v0.1  
> **适用阶段**: Phase 0 + Phase 1  
> **维护者**: Antigravity  
> **最后更新**: 2026-02-13

---

## 通用约定

| 项目 | 值 |
|------|-----|
| Base URL | `/api/v1` |
| Content-Type | `application/json; charset=UTF-8` |
| 文件上传 | `multipart/form-data` |
| 时间格式 | ISO 8601: `2026-02-13T20:00:00+08:00` |
| 分页参数 | `?page=0&size=20&sort=createdAt,desc` |
| 认证 | Phase 0-1: 无认证 |

### 统一响应格式

```json
{
  "code": 200,
  "message": "success",
  "data": {},
  "timestamp": "2026-02-13T20:00:00+08:00"
}
```

### 错误码约定

| code | 含义 |
|------|------|
| 200 | 成功 |
| 400 | 请求参数错误 |
| 404 | 资源不存在 |
| 409 | 资源冲突 (如重名) |
| 500 | 服务器内部错误 |

### 分页响应格式

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "content": [],
    "page": 0,
    "size": 20,
    "totalElements": 100,
    "totalPages": 5
  },
  "timestamp": "..."
}
```

---

## Phase 0: 基础接口

### `GET /api/v1/health`

健康检查接口。

**Response** `200`:
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

## Phase 1: 论文管理

### 1. 论文 CRUD

#### `GET /api/v1/papers`

获取论文列表（分页 + 筛选）。

**Query Parameters**:

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| page | int | 否 | 0 | 页码 (0-based) |
| size | int | 否 | 20 | 每页数量 |
| sort | string | 否 | `createdAt,desc` | 排序字段 |
| status | string | 否 | - | 阅读状态筛选: `UNREAD`, `SKIMMED`, `HALF_READ`, `FINISHED`, `NEED_REREAD` |
| tagId | long | 否 | - | 按标签 ID 筛选 |
| starred | boolean | 否 | - | 是否只看星标 |
| keyword | string | 否 | - | 搜索关键词 (匹配标题/作者/摘要) |

**Response** `200`:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "content": [
      {
        "id": 1,
        "title": "Attention Is All You Need",
        "authors": ["Vaswani, A.", "Shazeer, N."],
        "year": 2017,
        "venue": "NeurIPS",
        "doi": "10.48550/arXiv.1706.03762",
        "fileName": "attention_is_all_you_need.pdf",
        "fileSize": 2048576,
        "readingStatus": "FINISHED",
        "starred": true,
        "tags": [
          { "id": 1, "name": "NLP", "color": "#409EFF" },
          { "id": 2, "name": "Transformer", "color": "#67C23A" }
        ],
        "noteCount": 3,
        "createdAt": "2026-02-10T10:00:00+08:00",
        "updatedAt": "2026-02-13T15:30:00+08:00"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 42,
    "totalPages": 3
  },
  "timestamp": "..."
}
```

---

#### `POST /api/v1/papers`

新增论文（上传 PDF + 元数据）。

**Content-Type**: `multipart/form-data`

**Form Fields**:

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| file | File | 是 | PDF 文件 (最大 50MB) |
| title | string | 是 | 论文标题 |
| authors | string | 否 | 作者列表 (JSON 数组字符串, 如 `["Author A","Author B"]`) |
| year | int | 否 | 发表年份 |
| venue | string | 否 | 发表会议/期刊 |
| doi | string | 否 | DOI |
| abstractText | string | 否 | 摘要 |
| tagIds | string | 否 | 标签 ID 列表 (JSON 数组字符串, 如 `[1,2,3]`) |

**Response** `200`:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 2,
    "title": "BERT: Pre-training of Deep Bidirectional Transformers",
    "authors": ["Devlin, J."],
    "year": 2019,
    "venue": "NAACL",
    "doi": null,
    "fileName": "bert.pdf",
    "fileSize": 1523456,
    "readingStatus": "UNREAD",
    "starred": false,
    "tags": [],
    "noteCount": 0,
    "createdAt": "2026-02-13T20:00:00+08:00",
    "updatedAt": "2026-02-13T20:00:00+08:00"
  },
  "timestamp": "..."
}
```

---

#### `GET /api/v1/papers/{id}`

获取论文详情。

**Path Parameters**: `id` (long) — 论文 ID

**Response** `200`:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "title": "Attention Is All You Need",
    "authors": ["Vaswani, A.", "Shazeer, N."],
    "year": 2017,
    "venue": "NeurIPS",
    "doi": "10.48550/arXiv.1706.03762",
    "fileName": "attention_is_all_you_need.pdf",
    "fileSize": 2048576,
    "filePath": "/api/v1/papers/1/file",
    "readingStatus": "FINISHED",
    "starred": true,
    "abstractText": "The dominant sequence transduction models...",
    "tags": [
      { "id": 1, "name": "NLP", "color": "#409EFF" }
    ],
    "notes": [
      {
        "id": 1,
        "title": "核心思想",
        "sortOrder": 1,
        "createdAt": "2026-02-11T10:00:00+08:00",
        "updatedAt": "2026-02-12T14:00:00+08:00"
      }
    ],
    "noteCount": 1,
    "createdAt": "2026-02-10T10:00:00+08:00",
    "updatedAt": "2026-02-13T15:30:00+08:00"
  },
  "timestamp": "..."
}
```

**Response** `404`:
```json
{ "code": 404, "message": "论文不存在", "data": null, "timestamp": "..." }
```

---

#### `PUT /api/v1/papers/{id}`

更新论文元信息。

**Request Body** (`application/json`):
```json
{
  "title": "Updated Title",
  "authors": ["Author A", "Author B"],
  "year": 2020,
  "venue": "ICLR",
  "doi": "10.xxx",
  "abstractText": "Updated abstract...",
  "tagIds": [1, 3]
}
```

**Response** `200`: 同 `GET /api/v1/papers/{id}` 响应格式

---

#### `PATCH /api/v1/papers/{id}/status`

更新阅读状态。

**Request Body**:
```json
{
  "readingStatus": "HALF_READ"
}
```

**合法值**: `UNREAD`, `SKIMMED`, `HALF_READ`, `FINISHED`, `NEED_REREAD`

**Response** `200`: 同 `GET /api/v1/papers/{id}` 响应格式

---

#### `PATCH /api/v1/papers/{id}/star`

切换星标状态。

**Request Body**:
```json
{
  "starred": true
}
```

**Response** `200`: 同 `GET /api/v1/papers/{id}` 响应格式

---

#### `DELETE /api/v1/papers/{id}`

删除论文（同时删除关联笔记和文件）。

**Response** `200`:
```json
{ "code": 200, "message": "删除成功", "data": null, "timestamp": "..." }
```

---

#### `GET /api/v1/papers/{id}/file`

下载/预览论文 PDF 文件。

**Response**: `application/pdf` (二进制流)  
**Response Header**: `Content-Disposition: inline; filename="paper.pdf"`

---

### 2. 笔记 CRUD

#### `GET /api/v1/papers/{paperId}/notes`

获取某论文的全部笔记。

**Response** `200`:
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "paperId": 1,
      "title": "核心思想",
      "content": "## Self-Attention 机制\n\n这篇论文提出了...",
      "sortOrder": 1,
      "createdAt": "2026-02-11T10:00:00+08:00",
      "updatedAt": "2026-02-12T14:00:00+08:00"
    }
  ],
  "timestamp": "..."
}
```

---

#### `POST /api/v1/papers/{paperId}/notes`

新增笔记。

**Request Body**:
```json
{
  "title": "模型架构分析",
  "content": "## Encoder-Decoder\n\n..."
}
```

**Response** `200`:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 2,
    "paperId": 1,
    "title": "模型架构分析",
    "content": "## Encoder-Decoder\n\n...",
    "sortOrder": 2,
    "createdAt": "2026-02-13T20:00:00+08:00",
    "updatedAt": "2026-02-13T20:00:00+08:00"
  },
  "timestamp": "..."
}
```

---

#### `PUT /api/v1/notes/{noteId}`

更新笔记内容。

**Request Body**:
```json
{
  "title": "更新后的标题",
  "content": "## 更新后的内容\n\n新的分析...",
  "sortOrder": 1
}
```

**Response** `200`: 同创建时返回格式

---

#### `DELETE /api/v1/notes/{noteId}`

删除笔记。

**Response** `200`:
```json
{ "code": 200, "message": "删除成功", "data": null, "timestamp": "..." }
```

---

### 3. 标签 CRUD

#### `GET /api/v1/tags`

获取全部标签。

**Response** `200`:
```json
{
  "code": 200,
  "message": "success",
  "data": [
    { "id": 1, "name": "NLP", "color": "#409EFF", "paperCount": 12, "createdAt": "..." },
    { "id": 2, "name": "Security", "color": "#E6A23C", "paperCount": 5, "createdAt": "..." }
  ],
  "timestamp": "..."
}
```

---

#### `POST /api/v1/tags`

新增标签。

**Request Body**:
```json
{
  "name": "Deep Learning",
  "color": "#F56C6C"
}
```

**Response** `200`:
```json
{
  "code": 200,
  "message": "success",
  "data": { "id": 3, "name": "Deep Learning", "color": "#F56C6C", "paperCount": 0, "createdAt": "..." },
  "timestamp": "..."
}
```

**Response** `409` (标签名重复):
```json
{ "code": 409, "message": "标签名已存在", "data": null, "timestamp": "..." }
```

---

#### `PUT /api/v1/tags/{id}`

更新标签。

**Request Body**:
```json
{
  "name": "DL",
  "color": "#909399"
}
```

**Response** `200`: 同创建时返回格式

---

#### `DELETE /api/v1/tags/{id}`

删除标签（不删除关联论文，只移除标签关系）。

**Response** `200`:
```json
{ "code": 200, "message": "删除成功", "data": null, "timestamp": "..." }
```

---

## 附录: 阅读状态枚举

| 值 | 中文 | 说明 |
|-----|------|------|
| `UNREAD` | 未读 | 默认状态 |
| `SKIMMED` | 读了一点 | 浏览了摘要/引言 |
| `HALF_READ` | 读了一半 | 读了核心部分 |
| `FINISHED` | 精读完成 | 完整阅读 |
| `NEED_REREAD` | 需要重读 | 需要再次精读 |
