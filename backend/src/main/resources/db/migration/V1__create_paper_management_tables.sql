-- V1__create_paper_management_tables.sql
-- Phase 1: Paper Management Schema

-- Papers table
CREATE TABLE papers (
    id              BIGSERIAL PRIMARY KEY,
    title           VARCHAR(500) NOT NULL,
    authors         TEXT,
    year            INTEGER,
    venue           VARCHAR(200),
    doi             VARCHAR(200),
    file_path       VARCHAR(500),
    file_name       VARCHAR(300),
    file_size       BIGINT,
    reading_status  VARCHAR(20) NOT NULL DEFAULT 'UNREAD',
    starred         BOOLEAN NOT NULL DEFAULT FALSE,
    abstract_text   TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Notes table
CREATE TABLE notes (
    id          BIGSERIAL PRIMARY KEY,
    paper_id    BIGINT NOT NULL REFERENCES papers(id) ON DELETE CASCADE,
    title       VARCHAR(200) NOT NULL,
    content     TEXT,
    sort_order  INTEGER DEFAULT 0,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notes_paper_id ON notes(paper_id);

-- Tags table
CREATE TABLE tags (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(50) NOT NULL UNIQUE,
    color       VARCHAR(20) DEFAULT '#409EFF',
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Paper-Tag join table
CREATE TABLE paper_tags (
    paper_id    BIGINT NOT NULL REFERENCES papers(id) ON DELETE CASCADE,
    tag_id      BIGINT NOT NULL REFERENCES tags(id) ON DELETE CASCADE,
    PRIMARY KEY (paper_id, tag_id)
);

CREATE INDEX idx_paper_tags_tag_id ON paper_tags(tag_id);

-- Indexes for common queries
CREATE INDEX idx_papers_reading_status ON papers(reading_status);
CREATE INDEX idx_papers_starred ON papers(starred);
CREATE INDEX idx_papers_created_at ON papers(created_at DESC);
