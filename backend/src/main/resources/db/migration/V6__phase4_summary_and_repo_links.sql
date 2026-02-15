-- Phase 4: paper summaries + repo link extraction + storage key portability

ALTER TABLE papers ADD COLUMN IF NOT EXISTS file_storage_key VARCHAR(600);

CREATE TABLE IF NOT EXISTS paper_summaries (
    id               BIGSERIAL PRIMARY KEY,
    paper_id         BIGINT NOT NULL UNIQUE REFERENCES papers(id) ON DELETE CASCADE,
    status           VARCHAR(20) NOT NULL,
    provider         VARCHAR(20),
    model_name       VARCHAR(120),
    markdown_content TEXT,
    error_message    TEXT,
    generated_at     TIMESTAMPTZ,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS paper_repo_links (
    id          BIGSERIAL PRIMARY KEY,
    paper_id    BIGINT NOT NULL REFERENCES papers(id) ON DELETE CASCADE,
    url         VARCHAR(1000) NOT NULL,
    provider    VARCHAR(20) NOT NULL,
    status      VARCHAR(20) NOT NULL,
    source_text TEXT,
    page_no     INT,
    confidence  NUMERIC(4, 3),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_paper_repo_links_paper_url UNIQUE (paper_id, url)
);

CREATE INDEX IF NOT EXISTS idx_paper_summaries_status ON paper_summaries(status);
CREATE INDEX IF NOT EXISTS idx_paper_repo_links_paper_id ON paper_repo_links(paper_id);
CREATE INDEX IF NOT EXISTS idx_paper_repo_links_status ON paper_repo_links(status);
