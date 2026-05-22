package profile.users

import profile.infrastructure.db.DatabaseFactory
import profile.infrastructure.db.SqlLoader
import java.sql.ResultSet
import java.sql.Timestamp
import java.util.*

class UserRepository {
    private val createSql = SqlLoader.load("users/create_user.sql")
    private val findByEmailSql = SqlLoader.load("users/find_by_email.sql")
    private val findByIdSql = SqlLoader.load("users/find_by_id.sql")
    private val findByUsernameSql = SqlLoader.load("users/find_by_username.sql")

    fun create(user: User) {
        DatabaseFactory.getConnection().use { conn ->
            conn.prepareStatement(createSql).use { stmt ->
                stmt.setObject(1, UUID.fromString(user.id))
                stmt.setString(2, user.email)
                stmt.setString(3, user.username)
                stmt.setString(4, user.passwordHash)
                stmt.setString(5, user.firstName)
                stmt.setString(6, user.lastName)
                stmt.setString(7, user.displayName)
                stmt.setString(8, user.role)
                stmt.setString(9, user.status)
                stmt.setTimestamp(10, Timestamp.from(user.createdAt))
                stmt.setTimestamp(11, Timestamp.from(user.updatedAt))
                stmt.executeUpdate()
            }
            conn.commit()
        }
    }

    fun findByEmail(email: String): User? {
        DatabaseFactory.getConnection().use { conn ->
            conn.prepareStatement(findByEmailSql).use { stmt ->
                stmt.setString(1, email)
                val rs = stmt.executeQuery()
                if (rs.next()) return mapRow(rs)
            }
        }
        return null
    }

    fun findById(id: String): User? {
        DatabaseFactory.getConnection().use { conn ->
            conn.prepareStatement(findByIdSql).use { stmt ->
                stmt.setObject(1, UUID.fromString(id))
                val rs = stmt.executeQuery()
                if (rs.next()) return mapRow(rs)
            }
        }
        return null
    }

    fun findByUsername(username: String): User? {
        DatabaseFactory.getConnection().use { conn ->
            conn.prepareStatement(findByUsernameSql).use { stmt ->
                stmt.setString(1, username)
                val rs = stmt.executeQuery()
                if (rs.next()) return mapRow(rs)
            }
        }
        return null
    }

    fun findByIds(ids: List<String>): List<User> {
        if (ids.isEmpty()) return emptyList()
        val placeholders = ids.joinToString(",") { "?" }
        val sql = "SELECT * FROM users WHERE id IN ($placeholders)"
        
        val result = mutableListOf<User>()
        DatabaseFactory.getConnection().use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                ids.forEachIndexed { index, id ->
                    stmt.setObject(index + 1, UUID.fromString(id))
                }
                val rs = stmt.executeQuery()
                while (rs.next()) {
                    result.add(mapRow(rs))
                }
            }
        }
        return result
    }

    fun findAll(): List<User> {
        val result = mutableListOf<User>()
        DatabaseFactory.getConnection().use { conn ->
            conn.prepareStatement("SELECT * FROM users").use { stmt ->
                val rs = stmt.executeQuery()
                while (rs.next()) {
                    result.add(mapRow(rs))
                }
            }
        }
        return result
    }

    private fun mapRow(rs: ResultSet): User {
        return User(
            id = rs.getObject("id").toString(),
            email = rs.getString("email"),
            username = rs.getString("username"),
            passwordHash = rs.getString("password_hash"),
            emailVerified = rs.getBoolean("email_verified"),
            firstName = rs.getString("first_name"),
            lastName = rs.getString("last_name"),
            displayName = rs.getString("display_name"),
            avatarUrl = rs.getString("avatar_url"),
            bio = rs.getString("bio"),
            role = rs.getString("role"),
            status = rs.getString("status"),
            createdAt = rs.getTimestamp("created_at").toInstant(),
            updatedAt = rs.getTimestamp("updated_at").toInstant()
        )
    }
}
