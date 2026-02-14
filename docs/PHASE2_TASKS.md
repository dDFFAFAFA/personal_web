# Phase 2 任务下发 — 提示词

> **可以同时下发给 Codex 和 Gemini，两者互不依赖。**
> Antigravity 已创建好扩展字段、DTO、Flyway 迁移、CCF 数据文件。

---

## 🔧 给 Codex 的提示词

> 复制下方内容，直接粘贴给 Codex。

```
## 任务: [Phase 2] 后端 — 元数据增强 + BibTeX 导入导出

### 上下文

这是一个 Spring Boot 3.2 (Java 17) + Maven 的个人科研论文管理系统。
Phase 1 已完成基础 CRUD。Phase 2 需要添加三组功能：

1. **元数据自动填充** — 通过 DOI / 标题从外部 API 获取论文元信息
2. **期刊/会议级别查询** — 使用内置 CCF 数据文件查询 venue 的 CCF 等级
3. **BibTeX / RIS 导入导出** — 批量导入/导出论文引用

### Antigravity 已创建（只读引用，不要修改）

- `model/Paper.java` — 新增字段: `ccfRank`, `jcrQuartile`, `impactFactor`, `citationCount`, `paperUrl`
- `dto/response/VenueRankingResponse.java` — 期刊级别查询结果
- `dto/response/MetadataEnrichResponse.java` — 元数据填充预览结果
- `dto/request/DoiImportRequest.java` — DOI 导入请求
- `dto/response/PaperResponse.java` — 已扩展新字段
- `resources/data/ccf_venues.json` — CCF 推荐清单数据
- `resources/db/migration/V2__add_metadata_enrichment.sql` — Flyway 迁移

### 需要你创建的文件

#### 1. MetadataService.java (`service/`)

功能：
- `MetadataEnrichResponse enrichByDoi(String doi)` 
  - 调用 CrossRef API: `GET https://api.crossref.org/works/{doi}`
  - 解析返回 JSON，提取 title, authors, year, venue (container-title), abstract, DOI, URL
  - 同时调用 `VenueRankingService.lookup(venue)` 获取 CCF 等级
  - 超时 10 秒，失败时回退到 Semantic Scholar API

- `MetadataEnrichResponse enrichByTitle(String title)`
  - 调用 Semantic Scholar API: `GET https://api.semanticscholar.org/graph/v1/paper/search?query={title}&fields=title,authors,year,venue,abstract,citationCount,externalIds,url&limit=1`
  - 解析返回结果
  - 调用 `VenueRankingService.lookup(venue)` 获取 CCF 等级

- `void applyEnrichment(Long paperId, MetadataEnrichResponse response)`
  - 将元数据写入 Paper 实体的新字段中

使用 `RestTemplate` 或 `WebClient` 调用外部 API。
添加适当的重试和错误处理。

#### 2. VenueRankingService.java (`service/`)

功能：
- 启动时从 `classpath:data/ccf_venues.json` 加载数据到内存 Map
- `VenueRankingResponse lookup(String venue)` — 按名称匹配（支持模糊匹配和缩写匹配）
  - 先精确匹配 `name` 字段
  - 再精确匹配 `fullName` 字段
  - 最后做 contains 模糊匹配
  - 未找到返回 null

#### 3. BibTexService.java (`service/`)

功能：
- `String exportBibTeX(List<Long> paperIds)` — 将论文列表导出为 BibTeX 格式字符串
- `String exportRis(List<Long> paperIds)` — 将论文列表导出为 RIS 格式字符串  
- `List<PaperCreateRequest> parseBibTeX(InputStream input)` — 解析 BibTeX 文件，返回可创建论文的 DTO 列表
- `List<PaperCreateRequest> parseRis(InputStream input)` — 解析 RIS 文件

BibTeX 格式示例:
```bibtex
@article{vaswani2017attention,
  title={Attention is all you need},
  author={Vaswani, Ashish and Shazeer, Noam and others},
  journal={NeurIPS},
  year={2017},
  doi={10.48550/arXiv.1706.03762}
}
```

RIS 格式示例:
```
TY  - JOUR
TI  - Attention is all you need
AU  - Vaswani, Ashish
AU  - Shazeer, Noam
PY  - 2017
JO  - NeurIPS
DO  - 10.48550/arXiv.1706.03762
ER  - 
```

Maven 依赖已在 pom.xml 中，如果需要 BibTeX 解析库，请在 pom.xml 中添加:
```xml
<dependency>
    <groupId>org.jbibtex</groupId>
    <artifactId>jbibtex</artifactId>
    <version>1.0.20</version>
</dependency>
```

#### 4. MetadataController.java (`controller/`)

```
POST /api/v1/papers/enrich/doi     — body: DoiImportRequest → MetadataEnrichResponse
POST /api/v1/papers/enrich/title   — body: {"title": "xxx"} → MetadataEnrichResponse  
POST /api/v1/papers/{id}/enrich    — 对已有论文触发元数据补全，自动保存
GET  /api/v1/venues/lookup         — query: name=NeurIPS → VenueRankingResponse
```

#### 5. ImportExportController.java (`controller/`)

```
GET  /api/v1/papers/export         — query: format=bibtex|ris, ids=1,2,3 → 文件下载
POST /api/v1/papers/import         — multipart: file=xxx.bib → ApiResponse<List<PaperResponse>>
```

导出时设置 Content-Disposition header 为 attachment，文件名带时间戳。

#### 6. 修改 PaperService.java

- `createPaper()` 方法完成后，自动调用 `venueRankingService.lookup(venue)` 填充 ccfRank
- `toPaperResponse()` 方法需要映射 5 个新字段：ccfRank, jcrQuartile, impactFactor, citationCount, paperUrl

### 约束

- 遵守 OWNERSHIP.md 中的文件权限：不修改 model/、dto/、config/ 目录
- 所有新 API 统一返回 `ApiResponse<T>` 包装
- CrossRef/Semantic Scholar API 调用使用 polite 模式（带 User-Agent header）
- 外部 API 调用失败不应导致 500，应返回空结果并记录 WARN 日志

### 测试要求

- `VenueRankingServiceTest` — 测试精确匹配和模糊匹配
- `BibTexServiceTest` — 测试 BibTeX/RIS 解析和导出

### 交付

- 完成后将报告追加到 `docs/TASK_REPORT.md` 的 Phase 2 区域
```

---

## 🎨 给 Gemini 的提示词

> 复制下方内容，直接粘贴给 Gemini。

```
## 任务: [Phase 2] 前端 — 元数据增强 UI + Markdown 增强 + BibTeX 导入导出 + PDF 预览

### 上下文

这是一个 Vue 3 + TypeScript + Element Plus 的个人科研论文管理系统前端。
设计风格：**Google AI Studio 简朴科技风** + 左侧栏深色主题（Phase 1 已建立）。
Phase 2 需要在现有页面上增加四组功能。

### 一、元数据增强 UI

#### 1. 论文上传弹窗 (`PaperUploadDialog.vue`) 增强

在上传弹窗中增加一个 **"DOI 智能填充"** 区域：

```
┌─────────────────────────────────────────────┐
│  📥 上传论文                                 │
├─────────────────────────────────────────────┤
│  DOI: [10.xxxx/xxxxx          ] [🔍 自动填充]│
│  ─────────── 或手动填写 ───────────          │
│  标题: [自动填入的标题              ]        │
│  作者: [自动填入的作者              ]        │
│  年份: [2024]   期刊/会议: [NeurIPS]        │
│  CCF: [A ✅]    JCR: [Q1]                   │
│  ...                                         │
│         [取消]  [确认上传]                    │
└─────────────────────────────────────────────┘
```

- 点击"自动填充"后，调用 `POST /api/v1/papers/enrich/doi`
- 返回结果自动填入表单各字段
- CCF 等级显示颜色标签（A=红, B=橙, C=蓝）
- 如果 DOI 为空但有标题，可调用 `POST /api/v1/papers/enrich/title`

#### 2. 论文列表页 (`papers/index.vue`) 增强

- 每张论文卡片/表格行增加 CCF 等级小标签
  - `CCF-A` 红色 tag, `CCF-B` 橙色 tag, `CCF-C` 蓝色 tag
- 增加引用数显示（小字灰色，如 `📖 1234 citations`）
- 筛选栏增加 "CCF 等级" 下拉筛选（A / B / C / 全部）

#### 3. 论文详情页 (`papers/detail.vue`) 增强

- 信息区域增加显示：CCF 等级、JCR 分区、影响因子、引用数、论文链接
- 增加 "🔄 更新元数据" 按钮（调用 `POST /api/v1/papers/{id}/enrich`）

#### 新 API 调用 (`api/metadata.ts`)

```typescript
// 新建 api/metadata.ts
export const enrichByDoi = (doi: string) =>
  request.post<MetadataEnrichResponse>('/papers/enrich/doi', { doi })

export const enrichByTitle = (title: string) =>
  request.post<MetadataEnrichResponse>('/papers/enrich/title', { title })

export const enrichPaper = (paperId: number) =>
  request.post<PaperResponse>(`/papers/${paperId}/enrich`)

export const lookupVenue = (name: string) =>
  request.get<VenueRankingResponse>('/venues/lookup', { params: { name } })
```

#### 新 TypeScript 类型 (`types/paper.ts` 扩展)

```typescript
// 扩展 Paper 类型
interface Paper {
  // ...existing fields
  ccfRank?: string        // "A" | "B" | "C"
  jcrQuartile?: string    // "Q1" | "Q2" | "Q3" | "Q4"
  impactFactor?: number
  citationCount?: number
  paperUrl?: string
}

interface MetadataEnrichResponse {
  title: string
  authors: string[]
  year: number
  venue: string
  doi: string
  abstractText: string
  paperUrl: string
  citationCount: number
  ccfRank?: string
  jcrQuartile?: string
  source: string  // "crossref" | "semantic_scholar"
}

interface VenueRankingResponse {
  venue: string
  ccfRank?: string
  jcrQuartile?: string
  impactFactor?: number
  category: string
  type: string  // "conference" | "journal"
}
```

---

### 二、Markdown 编辑器增强

#### 笔记编辑页 (`papers/note-edit.vue`) 修改

1. **高亮语法支持**
   - 安装: `npm install markdown-it-mark`
   - 在 md-editor-v3 中通过 `markdownItPlugins` prop 注入
   - 用户输入 `==高亮文本==` 时，预览区应渲染为 `<mark>高亮文本</mark>`

2. **LaTeX 数学公式支持**
   - md-editor-v3 已内置 KaTeX 支持，确保 `:noKatex="false"` 
   - 支持行内 `$E=mc^2$` 和块级 `$$\sum_{i=1}^{n}$$`

3. **Mermaid 图表支持**
   - md-editor-v3 已内置，确保 `:noMermaid="false"`

4. **加深代码高亮主题** — 使用更醒目的代码主题

配置参考:
```vue
<MdEditor
  v-model="content"
  :theme="isDark ? 'dark' : 'light'"
  :previewTheme="'github'"
  :codeTheme="'a11y'"
  :showCodeRowNumber="true"
  :noKatex="false"
  :noMermaid="false"
  :markdownItPlugins="markdownItPlugins"
/>

<script setup>
import markdownItMark from 'markdown-it-mark'

const markdownItPlugins = [
  (md) => md.use(markdownItMark)
]
</script>
```

5. **添加高亮的 CSS 样式**
```css
mark {
  background-color: #fff3bf;
  padding: 0.1em 0.3em;
  border-radius: 3px;
}
/* dark mode */
.dark mark {
  background-color: #5c4d1a;
  color: #e5e7eb;
}
```

---

### 三、BibTeX / RIS 导入导出

#### 论文列表页 (`papers/index.vue`) 增加工具栏

在论文列表页顶部增加两个按钮:

```
[📥 导入] [📤 导出 BibTeX ▾]
```

**导入功能**：
- 点击"导入" → 弹出 el-upload 对话框，接受 `.bib` 和 `.ris` 文件
- 调用 `POST /api/v1/papers/import`（multipart/form-data）
- 成功后刷新列表，显示 "成功导入 N 篇论文"

**导出功能**：
- 下拉菜单: "导出为 BibTeX" / "导出为 RIS"  
- 如果有选中论文 → 导出选中项；否则 → 导出全部
- 调用 `GET /api/v1/papers/export?format=bibtex&ids=1,2,3`
- 将返回的文件下载到用户本地

#### 论文详情页 (`papers/detail.vue`) 增加

- 信息区增加 "📋 复制 BibTeX" 按钮
- 点击后调用导出 API（单篇），结果复制到剪贴板

#### 新 API 调用 (`api/importExport.ts`)

```typescript
export const exportPapers = (format: 'bibtex' | 'ris', ids?: number[]) =>
  request.get('/papers/export', {
    params: { format, ids: ids?.join(',') },
    responseType: 'blob'
  })

export const importPapers = (file: File) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post<PaperResponse[]>('/papers/import', formData)
}
```

---

### 四、PDF 在线预览

#### 论文详情页 (`papers/detail.vue`) 增加 PDF Tab

当论文有关联 PDF 文件时，详情页增加一个 "PDF 阅读" Tab:

```
[📋 详情] [📝 笔记(3)] [📄 PDF 阅读]
```

使用 `vue-pdf-embed` 或 `pdfjs-dist` 实现:
```bash
npm install vue-pdf-embed
```

功能要求:
- 全宽显示 PDF
- 支持缩放（+/- 按钮或 ctrl+scroll）
- 支持翻页
- 支持文本搜索
- 深色模式适配（PDF 背景）

如果 `vue-pdf-embed` 不支持上述全部功能，可以使用原始 `pdfjs-dist` + 自定义包装组件。

---

### 设计风格

延续 Phase 1 的 **Google AI Studio 简朴科技风**:
- 深色左侧栏
- 简洁卡片式布局
- Tag 使用 Element Plus 的 el-tag + 自定义颜色
- CCF 等级标签颜色: A=`#F56C6C`(红), B=`#E6A23C`(橙), C=`#409EFF`(蓝)

### 约束

- 遵守 OWNERSHIP.md，只修改 frontend/ 下的文件
- 组件使用 `<script setup>` + Composition API
- 新组件提取到独立文件，保持可复用性

### 交付

- 完成后将报告追加到 `docs/TASK_REPORT.md` 的 Phase 2 区域
```
