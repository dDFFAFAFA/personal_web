-- Phase 4 optimization: persist one-sentence summary for home cards

ALTER TABLE paper_summaries
    ADD COLUMN IF NOT EXISTS one_sentence VARCHAR(600);
