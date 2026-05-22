SELECT * FROM sessions WHERE refresh_token_hash = ? AND revoked_at IS NULL;
