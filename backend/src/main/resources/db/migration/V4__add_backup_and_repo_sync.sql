-- Phase 3: backup metadata + global repo sync config

ALTER TABLE papers ADD COLUMN oss_object_key VARCHAR(600);
ALTER TABLE papers ADD COLUMN backup_status VARCHAR(20) NOT NULL DEFAULT 'NOT_BACKED_UP';
ALTER TABLE papers ADD COLUMN backup_at TIMESTAMPTZ;
ALTER TABLE papers ADD COLUMN backup_error TEXT;

CREATE INDEX idx_papers_backup_status ON papers(backup_status);

CREATE TABLE repo_config (
    id               BIGINT PRIMARY KEY,
    provider         VARCHAR(20),
    repo_url         VARCHAR(500),
    branch_name      VARCHAR(100),
    target_dir       VARCHAR(500),
    last_sync_status VARCHAR(20),
    last_sync_mode   VARCHAR(20),
    last_sync_message TEXT,
    last_sync_at     TIMESTAMPTZ,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
