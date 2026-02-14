# Phase 4 Goals (Draft)

## G1. Production Migration + Multi-Device Access

### Target
- One-time migration from local dev machine to Linux server.
- After migration, all devices can upload/download through one deployed service endpoint.

### Must-have
- Stable deployment on Linux (`docker compose` + domain + HTTPS + gateway auth).
- Upload path mounted to persistent server disk.
- DB + file migration checklist/script (local -> server).
- Storage path portability:
  - avoid absolute local-machine path coupling
  - use logical storage key or server-relative path
- Recovery runbook (DB restore + file restore + OSS fallback).

### Acceptance
- Upload on device A, download on device B works.
- Service restart does not lose files or metadata.
- Migration rehearsal is repeatable.

## G2. DeepSeek Structured Summary (Home Display + Markdown Download)

### Target
- After configuring DeepSeek API in settings, user can generate structured paper summary from PDF.
- Summary shown on home page and downloadable as `.md`.

### Must-have
- Backend summary pipeline:
  - read PDF text
  - call LLM with structured prompt template
  - store generated markdown
- API:
  - `POST /api/v1/papers/{id}/summary/generate`
  - `GET /api/v1/papers/{id}/summary`
  - `GET /api/v1/papers/{id}/summary/download`
- Frontend:
  - trigger generation
  - show summary card/list on home page
  - markdown download button

### Acceptance
- Generate <= 60s for normal papers.
- Stored markdown can be reopened and downloaded.
- Failure states are visible with retry action.

## G3. Auto Extract GitHub/Gitee Links from PDF -> Repo Index

### Target
- Parse PDF text, extract GitHub/Gitee links, and add them into global code index workflow.

### Must-have
- Link extraction service (regex + normalization + de-dup).
- API:
  - `POST /api/v1/papers/{id}/repo-links/extract`
  - `GET /api/v1/papers/{id}/repo-links`
  - `POST /api/v1/papers/{id}/repo-links/apply`
- Apply step writes entries into `paper_code_entries` and triggers README rebuild.

### Acceptance
- Common GitHub/Gitee URL patterns are correctly extracted.
- Duplicate links are not inserted repeatedly.
- Extracted links can flow into unified repo management.
