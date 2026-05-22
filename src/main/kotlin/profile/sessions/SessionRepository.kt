package profile.sessions

import profile.infrastructure.db.DatabaseFactory
import profile.infrastructure.db.SqlLoader
import java.sql.ResultSet
import java.sql.Timestamp
import java.util.*

class SessionRepository {
    private val createSql = SqlLoader.load("sessions/create_session.sql")
    private val findByTokenHashSql = SqlLoader.load("sessions/find_by_token_hash.sql")
    private val findByUserIdSql = SqlLoader.load("sessions/find_by_user_id.sql")
    private val findActiveByIdsSql = SqlLoader.load("sessions/find_active_by_ids.sql")
    private val revokeSql = SqlLoader.load("sessions/revoke_session.sql")
    private val revokeAllForUserSql = SqlLoader.load("sessions/revoke_all_for_user.sql")
    private val updateExpirationSql = SqlLoader.load("sessions/update_session_expiration.sql")

    fun create(session: Session) {
        DatabaseFactory.getConnection().use { conn ->
            conn.prepareStatement(createSql).use { stmt ->
                stmt.setObject(1, UUID.fromString(session.id))
                stmt.setObject(2, UUID.fromString(session.userId))
                stmt.setString(3, session.refreshTokenHash)
                stmt.setString(4, session.deviceId)
                stmt.setString(5, session.userAgent)
                stmt.setString(6, session.ipAddress)
                stmt.setTimestamp(7, Timestamp.from(session.expiresAt))
                stmt.setTimestamp(8, Timestamp.from(session.lastUsedAt))
                stmt.setTimestamp(9, Timestamp.from(session.createdAt))
                stmt.executeUpdate()
            }
            conn.commit()
        }
    }

    fun findByTokenHash(hash: String): Session? {
        DatabaseFactory.getConnection().use { conn ->
            conn.prepareStatement(findByTokenHashSql).use { stmt ->
                stmt.setString(1, hash)
                val rs = stmt.executeQuery()
                if (rs.next()) return mapRow(rs)
            }
        }
        return null
    }

    fun findActiveByUserId(userId: String): List<Session> {
        val result = mutableListOf<Session>()
        DatabaseFactory.getConnection().use { conn ->
            conn.prepareStatement(findByUserIdSql).use { stmt ->
                stmt.setObject(1, UUID.fromString(userId))
                val rs = stmt.executeQuery()
                while (rs.next()) {
                    result.add(mapRow(rs))
                }
            }
        }
        return result
    }

    fun findActiveByIds(ids: List<String>): List<Session> {
        if (ids.isEmpty()) return emptyList()
        val result = mutableListOf<Session>()
        DatabaseFactory.getConnection().use { conn ->
            val array = conn.createArrayOf("UUID", ids.map { UUID.fromString(it) }.toTypedArray())
            conn.prepareStatement(findActiveByIdsSql).use { stmt ->
                stmt.setArray(1, array)
                val rs = stmt.executeQuery()
                while (rs.next()) {
                    result.add(mapRow(rs))
                }
            }
        }
        return result
    }

    fun revoke(sessionId: String) {
        DatabaseFactory.getConnection().use { conn ->
            conn.prepareStatement(revokeSql).use { stmt ->
                stmt.setObject(1, UUID.fromString(sessionId))
                stmt.executeUpdate()
            }
            conn.commit()
        }
    }

    fun revokeAllForUser(userId: String) {
        DatabaseFactory.getConnection().use { conn ->
            conn.prepareStatement(revokeAllForUserSql).use { stmt ->
                stmt.setObject(1, UUID.fromString(userId))
                stmt.executeUpdate()
            }
            conn.commit()
        }
    }

    fun updateExpiration(sessionId: String, expiresAt: java.time.Instant) {
        DatabaseFactory.getConnection().use { conn ->
            conn.prepareStatement(updateExpirationSql).use { stmt ->
                stmt.setTimestamp(1, Timestamp.from(expiresAt))
                stmt.setObject(2, UUID.fromString(sessionId))
                stmt.executeUpdate()
            }
            conn.commit()
        }
    }

    fun findById(id: String): Session? {
        DatabaseFactory.getConnection().use { conn ->
            conn.prepareStatement("SELECT * FROM sessions WHERE id = ?").use { stmt ->
                stmt.setObject(1, UUID.fromString(id))
                val rs = stmt.executeQuery()
                if (rs.next()) return mapRow(rs)
            }
        }
        return null
    }

    private fun mapRow(rs: ResultSet): Session {
        return Session(
            id = rs.getObject("id").toString(),
            userId = rs.getObject("user_id").toString(),
            refreshTokenHash = rs.getString("refresh_token_hash"),
            deviceId = rs.getString("device_id"),
            userAgent = rs.getString("user_agent"),
            ipAddress = rs.getString("ip_address"),
            expiresAt = rs.getTimestamp("expires_at").toInstant(),
            lastUsedAt = rs.getTimestamp("last_used_at").toInstant(),
            revokedAt = rs.getTimestamp("revoked_at")?.toInstant(),
            createdAt = rs.getTimestamp("created_at").toInstant()
        )
    }
}
