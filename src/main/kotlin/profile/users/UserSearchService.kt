package profile.users

import profile.infrastructure.redis.RedisManager
import java.util.UUID

class UserSearchService(
    private val userRepository: UserRepository
) {
    private fun getRedis() = try { RedisManager.sync() } catch (e: Exception) { null }
    private val indexKey = "users:idx:username"

    fun indexUser(user: User) {
        val redis = getRedis() ?: return
        val entry = "${user.username.lowercase()}:${user.id}"
        redis.zadd(indexKey, 0.0, entry)
    }

    fun indexAllUsers() {
        val redis = getRedis() ?: return
        val users = userRepository.findAll()
        users.forEach { indexUser(it) }
    }

    fun searchByUsernamePrefix(prefix: String, limit: Int = 10): List<UserPublicDto> {
        if (prefix.isBlank()) return emptyList()
        val redis = getRedis() ?: return emptyList()

        val results = redis.zrangebylex(
            indexKey, 
            io.lettuce.core.Range.from(
                io.lettuce.core.Range.Boundary.including(prefix.lowercase()), 
                io.lettuce.core.Range.Boundary.excluding(prefix.lowercase() + "\u00ff")
            ), 
            io.lettuce.core.Limit.create(0, limit.toLong())
        )

        val ids = results.map { it.split(":").last() }
        if (ids.isEmpty()) return emptyList()
        
        return userRepository.findByIds(ids).map { it.toPublicDto() }
    }

    fun getUserIdByUsername(username: String): String? {
        val user = userRepository.findByUsername(username)
        return user?.id
    }
}
