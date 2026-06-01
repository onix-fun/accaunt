ALTER TABLE sessions
    ADD COLUMN previous_refresh_token_hash TEXT;

ALTER TABLE sessions
    ADD COLUMN refresh_token_rotated_at TIMESTAMP WITH TIME ZONE;

CREATE INDEX idx_sessions_previous_refresh_token_hash
    ON sessions(previous_refresh_token_hash);
