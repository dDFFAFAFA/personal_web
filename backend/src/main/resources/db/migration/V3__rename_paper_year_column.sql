-- Rename reserved column name for portability (H2/Postgres)
ALTER TABLE papers RENAME COLUMN year TO publish_year;
