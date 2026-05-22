UPDATE sessions SET expires_at = ?, last_used_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP WHERE id = ?;
