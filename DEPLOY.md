# Deployment Guide

## 1. Prerequisites
- Docker 24+
- Docker Compose v2

## 2. Start Full Stack
Run at project root:

```bash
docker compose up -d --build
```

Services:
- Frontend: `http://localhost`
- Backend API: `http://localhost:8080/api/v1`
- PostgreSQL: `localhost:5432`

## 3. Stop Services
```bash
docker compose down
```

Data persists in:
- Database: `./data/db`
- Upload files: `./data/uploads`

## 4. Verify Deployment
### 4.1 Check containers
```bash
docker compose ps
```

### 4.2 Check backend health
```bash
curl http://localhost:8080/api/v1/health
```

### 4.3 Verify upload persistence
1. Upload a PDF from the web UI.
2. Confirm file exists under `./data/uploads`.
3. Restart stack:
```bash
docker compose restart
```
4. Confirm uploaded file and metadata are still available.

## 5. Production Config Notes
- Backend runs with `SPRING_PROFILES_ACTIVE=prod`.
- Database host is `postgres` (Docker service name).
- Upload path is controlled by env var `UPLOAD_PATH` (default `/app/uploads/papers` in prod profile).
