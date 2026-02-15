-- Phase 3 extension: paper code entries + AI provider config

ALTER TABLE repo_config ADD COLUMN IF NOT EXISTS auto_commit_readme BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE repo_config ADD COLUMN IF NOT EXISTS app_base_url VARCHAR(500);

CREATE TABLE IF NOT EXISTS paper_code_entries (
    id          BIGSERIAL PRIMARY KEY,
    paper_id    BIGINT NOT NULL REFERENCES papers(id) ON DELETE CASCADE,
    repo_url    VARCHAR(500) NOT NULL,
    branch_name VARCHAR(100),
    provider    VARCHAR(20),
    description TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_paper_code_entries_paper_id ON paper_code_entries(paper_id);
CREATE INDEX IF NOT EXISTS idx_paper_code_entries_updated_at ON paper_code_entries(updated_at DESC);

CREATE TABLE IF NOT EXISTS ai_provider_config (
    provider    VARCHAR(20) PRIMARY KEY,
    enabled     BOOLEAN NOT NULL DEFAULT FALSE,
    base_url    VARCHAR(500),
    model_name  VARCHAR(120),
    api_key     VARCHAR(500),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
