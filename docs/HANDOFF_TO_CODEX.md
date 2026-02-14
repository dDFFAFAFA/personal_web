# Handoff to New Codex Agent

**Role**: You are a new Codex agent (Backend/DevOps focus) taking over the project.
**Goal**: Complete **Phase 3: Deployment & Final Polish**.

---

## 📅 Project Status

- **Phase 0 (Foundation)**: ✅ Done. Spring Boot + Vue 3 scaffold, Git repo, API contract.
- **Phase 1 (Paper Management)**: ✅ Done. CRUD for papers/notes/tags, file upload.
- **Phase 2 (Enhancement)**: ✅ Done.
  - **Metadata**: DOI auto-fill (`/papers/enrich/*`), CCF venue ranking.
  - **Import/Export**: BibTeX/RIS support (`jbibtex`).
  - **Frontend**: Dark mode, PDF preview, Markdown highlighting.

**Current Tech Stack**:
- **Backend**: Spring Boot 3.2, Java 17, H2 Database (Dev), PostgreSQL (Prod - planned).
- **Frontend**: Vue 3, Vite, Element Plus, TypeScript.
- **Repository**: Git repository is active at `/Users/changye/Desktop/2026_LLM-Agent/vibe-coding/personal_web`.
- **Docs**: `docs/` contains `TASK_REPORT.md`, `API_CONTRACT.md`.

---

## 🚀 Your Task: Phase 3 (Deployment & Polish)

The application works locally (`mvn spring-boot:run` + `npm run dev`). Now we need to make it **deployable** and **production-ready**.

### 1. Docker & Orchestration
- [ ] Create `docker-compose.yml` at project root:
  - **PostgreSQL 16**: Replace H2. persistent volume `./data/db`.
  - **MinIO** (Optional but recommended): For file storage, or just mapped volume `./data/uploads`.
  - **Backend**: Build from `backend/Dockerfile`. Map port 8080.
  - **Frontend**: Build from `frontend/Dockerfile` (Nginx serving static dist). Map port 80.
- [ ] Create `nginx/nginx.conf`:
  - Reverse proxy `/api/` to Backend container.
  - Serve Frontend static files.
  - Gzip compression.

### 2. Configuration (`application-prod.yml`)
- [ ] Ensure `backend/src/main/resources/application-prod.yml` exists and is configured for Docker environment (DB host = `postgres`, not `localhost`).
- [ ] Ensure file upload path is configurable via env var.

### 3. Documentation
- [ ] Create `DEPLOY.md`: One-click deployment guide (`docker-compose up -d`).

### 4. Final Verification
- [ ] Run the full stack with Docker.
- [ ] Verify file upload works (permissions).
- [ ] Verify data persistence (restart container, data remains).

---

## ⚠️ Constraints
- **Do NOT** modify existing business logic (Java/Vue files) unless fixing a deployment bug.
- **Do** respect `OWNERSHIP.md`. You own `docker-compose.yml`, `nginx/`, and `backend/Dockerfile`.
- **Git**: Commit your changes with clear messages (e.g., `feat(deploy): add docker-compose`).
