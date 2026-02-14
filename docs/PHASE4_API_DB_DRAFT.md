# Phase4 API + DB Draft

## Scope
- Migration readiness for cross-device upload/download on Linux deployment.
- DeepSeek structured summary generation from PDF -> home display -> markdown download.
- Extract GitHub/Gitee links from PDF text and flow into unified repo index.

## API Draft

### A. Migration & Storage Readiness

1. `POST /api/v1/admin/storage/precheck`
- Purpose: verify upload base path, read/write permission, DB/file consistency.
- Response:
  - `writable` (bool)
  - `missingFilePaperIds` (array)
  - `outsideBasePathPaperIds` (array)
  - `suggestedActions` (array)

2. `POST /api/v1/admin/storage/rewrite-paths`
- Purpose: rewrite legacy absolute `file_path` to server-local relative key (admin only).
- Body:
  - `fromPrefix`
  - `toPrefix`
  - `dryRun` (bool)
- Response:
  - `affectedCount`
  - `updatedPaperIds`

### B. Structured Summary (DeepSeek/Qwen compatible config)

1. `POST /api/v1/papers/{id}/summary/generate`
- Purpose: parse PDF text + call enabled LLM provider to generate structured markdown summary.
- Body:
  - `provider` (optional, default: enabled provider priority DeepSeek -> Qwen)
  - `forceRegenerate` (optional)
- Response:
  - `paperId`
  - `status` (`QUEUED|GENERATING|SUCCESS|FAILED`)
  - `summaryId`
  - `message`

2. `GET /api/v1/papers/{id}/summary`
- Response:
  - `paperId`
  - `status`
  - `provider`
  - `model`
  - `markdown`
  - `generatedAt`
  - `error`

3. `GET /api/v1/papers/{id}/summary/download`
- Response: markdown file download (`Content-Disposition: attachment; filename="paper_{id}_summary.md"`).

4. `GET /api/v1/home/summaries?limit=10`
- Purpose: home page summary cards/list.

### C. Repo Link Extraction from PDF

1. `POST /api/v1/papers/{id}/repo-links/extract`
- Purpose: scan PDF text and extract GitHub/Gitee links.
- Response:
  - `paperId`
  - `candidateCount`
  - `candidates` (id/url/provider/confidence/sourceText/pageNo)

2. `GET /api/v1/papers/{id}/repo-links`
- Purpose: list extracted candidates + apply state.

3. `POST /api/v1/papers/{id}/repo-links/apply`
- Body:
  - `candidateIds` (array)
  - `autoRebuildReadme` (bool, default true)
- Effect:
  - writes selected links to `paper_code_entries`
  - triggers README rebuild flow

4. `DELETE /api/v1/papers/{id}/repo-links/{candidateId}`
- Purpose: reject/delete wrong candidate.

## DB Draft (Flyway)

### `V6__phase4_summary_and_repo_links.sql`

1. `paper_summaries`
- `id BIGSERIAL PK`
- `paper_id BIGINT UNIQUE NOT NULL REFERENCES papers(id) ON DELETE CASCADE`
- `status VARCHAR(20) NOT NULL` (`PENDING|GENERATING|SUCCESS|FAILED`)
- `provider VARCHAR(20)` (`DEEPSEEK|QWEN`)
- `model_name VARCHAR(120)`
- `markdown_content TEXT`
- `error_message TEXT`
- `generated_at TIMESTAMPTZ`
- `created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()`
- `updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()`

2. `paper_repo_links`
- `id BIGSERIAL PK`
- `paper_id BIGINT NOT NULL REFERENCES papers(id) ON DELETE CASCADE`
- `url VARCHAR(1000) NOT NULL`
- `provider VARCHAR(20) NOT NULL` (`GITHUB|GITEE|UNKNOWN`)
- `status VARCHAR(20) NOT NULL` (`CANDIDATE|APPLIED|REJECTED`)
- `source_text TEXT`
- `page_no INT`
- `confidence NUMERIC(4,3)`
- `created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()`
- `updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()`
- unique index: `(paper_id, url)`

3. `papers` portability fields
- add `file_storage_key VARCHAR(600)` (server-relative logical path, preferred over absolute path)
- keep `file_path` for backward compatibility during migration window

## Error Contract
- Validation/config/input issues -> `400`
- Unauthorized/forbidden (gateway/admin) -> `401/403`
- Missing resources -> `404`
- LLM/OSS/repo upstream unavailable -> `502/503` (or mapped `500` with readable message if needed)

## Frontend Contract Notes
- Home page calls `GET /api/v1/home/summaries`.
- Paper detail page adds:
  - generate summary button
  - view markdown block
  - download markdown button
  - repo-link extraction + apply panel
