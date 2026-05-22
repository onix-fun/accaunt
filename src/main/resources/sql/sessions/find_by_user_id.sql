SELECT * FROM sessions WHERE user_id = ? AND revoked_at IS NULL AND expires_at > CURRENT_TIMESTAMP;
