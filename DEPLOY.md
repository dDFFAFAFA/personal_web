# Deployment Guide (Phase 3)

## 1. Prerequisites
- Docker 24+
- Docker Compose v2
- Linux server directories:
  - `/srv/personal-web/uploads/`
  - `/srv/personal-web/repos/`
  - `/etc/personal-web/keys/`

## 2. Environment Setup
Create `.env` in project root (example):

```bash
OSS_ENDPOINT=https://oss-cn-hangzhou.aliyuncs.com
OSS_BUCKET=your-bucket
OSS_REGION=cn-hangzhou
OSS_ACCESS_KEY_ID=your-ak
OSS_ACCESS_KEY_SECRET=your-sk
```

Prepare SSH private key for repo sync:

```bash
mkdir -p ./data/keys
cp /path/to/id_ed25519 ./data/keys/id_ed25519
chmod 600 ./data/keys/id_ed25519
```

## 3. Start Full Stack

```bash
docker compose up -d --build
```

Services:
- Frontend: `http://localhost`
- Backend API: `http://localhost:8080/api/v1`
- PostgreSQL: `localhost:5432`

## 4. Verify

### 4.1 Container status
```bash
docker compose ps
```

### 4.2 Health check
```bash
curl http://localhost:8080/api/v1/health
```

### 4.3 Upload + persistence
1. Upload a PDF from UI.
2. Confirm file exists under `./data/uploads`.
3. Restart:
```bash
docker compose restart
```
4. Confirm file is still accessible.

### 4.4 Manual OSS backup
1. Open paper detail and click `备份到 OSS`.
2. Or call API:
```bash
curl -X POST http://localhost:8080/api/v1/papers/{id}/backup
```

### 4.5 Repo sync
1. Open `系统设置` and save global repo config (GitHub/Gitee SSH URL).
2. Click `同步仓库（Clone/Pull）`.

## 5. Stop

```bash
docker compose down
```

Data persists in:
- DB: `./data/db`
- Uploads: `./data/uploads`
- Repos: `./data/repos`

## 6. Gateway Auth (Nginx)
`nginx/nginx.conf` includes optional `auth_basic` sample lines.
Enable by mounting `.htpasswd` into frontend container and uncommenting those lines.
