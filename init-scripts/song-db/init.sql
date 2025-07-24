CREATE TABLE IF NOT EXISTS metadata (
    id BIGSERIAL PRIMARY KEY,
    resource_id BIGINT NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    artist VARCHAR(255) NOT NULL,
    album VARCHAR(255) NOT NULL,
    duration SMALLINT NOT NULL,
    year DATE NOT NULL
);

COMMENT ON COLUMN metadata.duration IS 'Duration in seconds';
COMMENT ON COLUMN metadata.year IS 'Release year (date used for year-only storage)';