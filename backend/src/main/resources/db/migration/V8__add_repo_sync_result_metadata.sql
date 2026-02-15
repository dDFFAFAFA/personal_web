-- Persist repo sync result metadata for stable status tracing (backward compatible)
ALTER TABLE repo_config ADD COLUMN IF NOT EXISTS last_sync_error_code INTEGER;
ALTER TABLE repo_config ADD COLUMN IF NOT EXISTS last_sync_duration_ms BIGINT;
