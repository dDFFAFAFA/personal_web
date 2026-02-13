# Phase 1 任务下发 — 提示词

> **可以同时下发给 Codex 和 Gemini，两者互不依赖。**  
> Antigravity 已创建好 `model/`、`dto/`、Flyway 迁移脚本，Agent 只读引用。

---

## 🔧 给 Codex 的提示词（后端）

```
你是这个项目的后端开发者。请先阅读 CONTRACT.md、OWNERSHIP.md 和 docs/API_CONTRACT.md，然后完成 Phase 1 后端业务逻辑开发。

### 任务：[Phase 1] 论文管理系统后端

### 上下文
- Antigravity 已创建好所有实体类 (model/)、DTO (dto/)、Flyway 迁移脚本
- 你只需实现 Repository、Service、Controller 层
- 接口定义严格参考 docs/API_CONTRACT.md（Phase 1 部分）
- 实体类和 DTO 是只读引用，不可修改

### 具体要求

#### 1. Repository 层 (`repository/`)

**PaperRepository.java**：
- 继承 `JpaRepository<Paper, Long>`
- 支持按 readingStatus 筛选：`findByReadingStatus(ReadingStatus status, Pageable pageable)`
- 支持按 starred 筛选：`findByStarred(Boolean starred, Pageable pageable)`
- 支持关键词搜索（标题/作者/摘要模糊匹配）：用 `@Query` 实现
- 支持按 tagId 筛选：JOIN paper_tags 查询
- 组合筛选查询（status + tagId + starred + keyword 任意组合），建议使用 Spring Data JPA Specification

**NoteRepository.java**：
- 继承 `JpaRepository<Note, Long>`
- `findByPaperIdOrderBySortOrderAsc(Long paperId)`
- `countByPaperId(Long paperId)`

**TagRepository.java**：
- 继承 `JpaRepository<Tag, Long>`
- `findByName(String name)` 用于重名检查
- 查标签及其关联论文数量（可用 @Query 或在 Service 层计算）

#### 2. Service 层 (`service/`)

**PaperService.java**：
- `Page<PaperResponse> listPapers(String keyword, ReadingStatus status, Long tagId, Boolean starred, Pageable pageable)` — 分页列表，支持组合筛选
- `PaperResponse getPaper(Long id)` — 详情（含笔记摘要列表）
- `PaperResponse createPaper(MultipartFile file, PaperCreateRequest request)` — 创建论文 + 保存 PDF 文件
- `PaperResponse updatePaper(Long id, PaperUpdateRequest request)` — 更新元信息
- `PaperResponse updateStatus(Long id, StatusUpdateRequest request)` — 更新阅读状态
- `PaperResponse toggleStar(Long id, StarUpdateRequest request)` — 切换星标
- `void deletePaper(Long id)` — 删除论文 + 删除关联文件
- `Resource getPaperFile(Long id)` — 获取 PDF 文件流

文件存储逻辑：
- 从 application.yml 中读取 `app.upload.path` 配置
- 文件保存命名：`{paperId}_{originalFilename}`
- 删除论文时同步删除文件

实体 → DTO 转换：
- authors 字段：实体中存的是 JSON 字符串，需要序列化/反序列化为 `List<String>`
- 使用 com.fasterxml.jackson.databind.ObjectMapper 解析 authors

**NoteService.java**：
- `List<NoteResponse> listNotes(Long paperId)` — 某论文的笔记列表
- `NoteResponse createNote(Long paperId, NoteCreateRequest request)` — 新增笔记
- `NoteResponse updateNote(Long noteId, NoteUpdateRequest request)` — 更新笔记
- `void deleteNote(Long noteId)` — 删除笔记

**TagService.java**：
- `List<TagResponse> listTags()` — 全部标签（含 paperCount）
- `TagResponse createTag(TagCreateRequest request)` — 新增（重名返回 409）
- `TagResponse updateTag(Long id, TagCreateRequest request)` — 更新
- `void deleteTag(Long id)` — 删除标签（仅解除关系，不删论文）

#### 3. Controller 层 (`controller/`)

**PaperController.java**：
- 严格按 API_CONTRACT.md 中的路径、方法、参数实现
- POST /api/v1/papers：接收 @RequestPart MultipartFile file + @RequestPart PaperCreateRequest (JSON)。或者用 @RequestParam 逐字段接收 form-data 再手动构造 request（参考 CONTRACT 中的 multipart/form-data 约定）
- 所有返回统一用 `ApiResponse<T>` 封装
- 使用 @Validated 做参数校验

**NoteController.java**：
- GET /api/v1/papers/{paperId}/notes
- POST /api/v1/papers/{paperId}/notes
- PUT /api/v1/notes/{noteId}
- DELETE /api/v1/notes/{noteId}

**TagController.java**：
- GET /api/v1/tags
- POST /api/v1/tags
- PUT /api/v1/tags/{id}
- DELETE /api/v1/tags/{id}

#### 4. 单元测试 (`test/`)

- PaperService 的核心方法测试（Mockito mock Repository）
- PaperController 的 MockMvc 测试（至少覆盖 list/create/get）
- TagService 重名冲突测试

### Git 规范
- 在 develop 分支上开发，commit 使用 `feat(paper): ...` / `feat(note): ...` / `feat(tag): ...`

### 约束
- 严格遵守 OWNERSHIP.md 权限：不可修改 model/、dto/、config/ 下的文件
- Controller 统一返回 ApiResponse<T>
- 使用 @Slf4j 记录关键操作日志
- 业务异常使用 BusinessException，由 GlobalExceptionHandler 统一处理

### 交付标准
- [ ] `mvn clean package -DskipTests` 构建成功
- [ ] `mvn test` 测试通过
- [ ] Health API + Paper CRUD API 在本地可调通
- [ ] 文件上传/下载功能正常

### 完成后的报告要求
任务完成后，请将你的工作报告追加到 `docs/TASK_REPORT.md` 文件中 Phase 1 区域。
请按照文件底部的报告模板格式填写，包括：完成的工作清单、运行结果、遇到的问题、建议、文件列表。
```

---

## 🎨 给 Gemini 的提示词（前端）

```
你是这个项目的前端开发者。请先阅读 CONTRACT.md、OWNERSHIP.md 和 docs/API_CONTRACT.md，然后完成 Phase 1 前端页面开发。

### 任务：[Phase 1] 论文管理系统前端

### 上下文
- 后端 API 尚在开发中，请使用 Mock 数据先行开发
- 接口格式严格参考 docs/API_CONTRACT.md（Phase 1 部分）
- 后端 DTO 类型参考 backend/src/main/java/com/changye/web/dto/（只读）
- 已安装的依赖：vue3, element-plus, axios, pinia, vue-router
- 需要额外安装：md-editor-v3（Markdown 编辑器）

### 设计风格 — Google AI Studio 简朴科技风

请参考 Google AI Studio 的设计语言：
- **整体**：极简、干净、留白充足，无多余装饰
- **配色**：浅色背景 (#f8f9fa / #ffffff)，深灰文字 (#202124 / #5f6368)，蓝色强调 (#1a73e8)
- **卡片**：圆角 12px，极细边框 (#e0e0e0) 或无边框 + 轻微阴影
- **侧边栏**：白底 + 浅灰分隔线，图标 + 文字简洁排列（非深色背景）
- **顶栏**：白底，左侧 Logo/标题，右侧用户操作区
- **按钮**：圆角药丸按钮，主色 #1a73e8，悬停加深
- **状态标签**：柔和的淡色背景 + 深色文字（如淡蓝底+蓝字）
- **字体**：Google Sans 或 system-ui，字号适中，行间距宽松
- **动效**：微妙的过渡 (0.2s ease)，无花哨动画
- **表格/列表**：清爽的行间距，hover 浅灰高亮

> 关键原则：像 Google 产品一样，让界面看起来"什么都没有设计过"但用起来非常舒服。

### 具体要求

#### 1. 更新全局样式和布局

**更新 `styles/index.css`**：
- 替换当前深蓝色系为 Google AI Studio 风格（浅色、极简）
- CSS 变量定义统一的色彩系统

**更新 `layouts/DefaultLayout.vue`**：
- 侧边栏改为白底 + 浅灰边线风格（不再是深色背景）
- 顶栏保持白底，左侧 Hamburger + 「ChangYe 科研工具台」
- 侧边栏菜单项：首页、论文管理（启用，不再 disabled）、标签管理

#### 2. TypeScript 类型定义 (`types/`)

在 `types/paper.ts` 中定义：

```typescript
import type { ReadingStatus } from './enums'

export interface Paper {
  id: number
  title: string
  authors: string[]
  year?: number
  venue?: string
  doi?: string
  fileName?: string
  fileSize?: number
  readingStatus: ReadingStatus
  starred: boolean
  tags: Tag[]
  noteCount: number
  createdAt: string
  updatedAt: string
}

// 同理定义 Note, Tag, PaperDetail 等
```

在 `types/enums.ts` 中定义：

```typescript
export type ReadingStatus = 'UNREAD' | 'SKIMMED' | 'HALF_READ' | 'FINISHED' | 'NEED_REREAD'

export const ReadingStatusLabel: Record<ReadingStatus, string> = {
  UNREAD: '未读',
  SKIMMED: '读了一点',
  HALF_READ: '读了一半',
  FINISHED: '精读完成',
  NEED_REREAD: '需要重读',
}

export const ReadingStatusColor: Record<ReadingStatus, string> = {
  UNREAD: '#9e9e9e',
  SKIMMED: '#2196f3',
  HALF_READ: '#ff9800',
  FINISHED: '#4caf50',
  NEED_REREAD: '#f44336',
}
```

#### 3. API 封装 (`api/`)

**`api/paper.ts`**：封装论文 CRUD API（参考 API_CONTRACT.md）
**`api/note.ts`**：封装笔记 CRUD API
**`api/tag.ts`**：封装标签 CRUD API

开发阶段可先 mock 返回值。

#### 4. 页面开发 (`pages/`)

**论文列表页 `pages/papers/index.vue`**：
- 顶部工具栏：搜索框 + 阅读状态筛选 (el-select) + 标签筛选 + 星标筛选 + 「上传论文」按钮
- 支持列表/卡片视图切换
- 列表视图：el-table 展示论文（标题、作者、年份、状态标签、星标、标签胶囊）
- 卡片视图：el-card 网格排列
- 分页：el-pagination
- 行操作：编辑信息、修改状态（快速 dropdown）、删除
- 星标：点击切换

**论文上传对话框 `pages/papers/components/PaperUploadDialog.vue`**：
- el-dialog 弹窗
- el-upload 拖拽上传 PDF
- 表单字段：标题(必填)、作者、年份、会议/期刊、DOI、摘要、标签选择
- 提交后调用 POST /api/v1/papers

**论文详情页 `pages/papers/detail.vue`**：
- 左侧：论文元信息卡片（标题、作者、年份、状态下拉切换、标签、星标）
- 右侧：笔记列表
- 笔记可点击进入编辑
- 「新增笔记」按钮
- 底部或侧边：PDF 预览入口（链接到 /api/v1/papers/{id}/file）

**笔记编辑页 `pages/papers/note-edit.vue`**：
- 安装 md-editor-v3：`npm install md-editor-v3`
- 全屏 Markdown 编辑器，上方标题输入
- 实时预览（双栏模式）
- 保存按钮，自动保存提示
- 参考语雀的编辑体验

**标签管理页 `pages/tags/index.vue`**：
- 标签列表（名称 + 颜色圆点 + 关联论文数）
- 新增/编辑/删除标签
- 颜色选择器 (el-color-picker)

#### 5. 路由更新 (`router/index.ts`)

```typescript
{ path: '/papers', name: 'Papers', component: PaperList },
{ path: '/papers/:id', name: 'PaperDetail', component: PaperDetail },
{ path: '/papers/:id/notes/:noteId', name: 'NoteEdit', component: NoteEdit },
{ path: '/tags', name: 'Tags', component: TagManagement },
```

#### 6. Pinia Store (`stores/`)

**`stores/paperStore.ts`**：论文列表状态、筛选条件、分页
**`stores/tagStore.ts`**：标签列表（全局缓存供多页面使用）

### Git 规范
- 在 develop 分支上开发
- `feat(frontend): ...` / `feat(ui): ...`

### 约束
- 严格遵守 OWNERSHIP.md，只修改 frontend/ 下的文件
- 使用 <script setup lang="ts">
- 所有页面组件加 data-testid
- Element Plus 已有按需导入配置，直接使用组件即可

### 交付标准
- [ ] `npm run dev` 开发服务器正常运行
- [ ] 论文列表页可展示（mock 数据）
- [ ] 论文上传弹窗可打开，表单可填写
- [ ] Markdown 编辑器可正常加载和输入
- [ ] `npm run build` 构建成功
- [ ] 整体风格符合 Google AI Studio 简朴科技感

### 完成后的报告要求
任务完成后，请将你的工作报告追加到 `docs/TASK_REPORT.md` 文件中 Phase 1 区域。
请按照文件底部的报告模板格式填写，包括：完成的工作清单、运行结果、遇到的问题、建议、文件列表。
```

---

## ⚡ 下发顺序

```
1. 同时下发给 Codex 和 Gemini（完全独立）
2. 等两边都完成后，告诉 Antigravity: "他们干完了"
3. Antigravity 自动读取 docs/TASK_REPORT.md → 审查 → 集成联调
```
