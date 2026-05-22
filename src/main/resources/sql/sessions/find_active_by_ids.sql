SELECT * FROM sessions WHERE id = ANY(?) AND revoked_at IS NULL AND expires_at > CURRENT_TIMESTAMP;
