-- V2__add_metadata_enrichment.sql
-- Phase 2A: Metadata Enrichment Fields

ALTER TABLE papers ADD COLUMN ccf_rank VARCHAR(10);
ALTER TABLE papers ADD COLUMN jcr_quartile VARCHAR(10);
ALTER TABLE papers ADD COLUMN impact_factor DECIMAL(6,3);
ALTER TABLE papers ADD COLUMN citation_count INTEGER DEFAULT 0;
ALTER TABLE papers ADD COLUMN paper_url VARCHAR(500);

-- Index for common filter queries
CREATE INDEX idx_papers_ccf_rank ON papers(ccf_rank);
CREATE INDEX idx_papers_jcr_quartile ON papers(jcr_quartile);
