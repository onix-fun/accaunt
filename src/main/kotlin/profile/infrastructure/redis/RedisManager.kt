package profile.infrastructure.redis

import io.lettuce.core.RedisClient
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.sync.RedisCommands
import io.ktor.server.config.*

object RedisManager {
    private var client: RedisClient? = null
    private var connection: StatefulRedisConnection<String, String>? = null

    fun init(config: ApplicationConfig) {
        val url = config.propertyOrNull("redis.url")?.getString()
        if (url != null) {
            try {
                client = RedisClient.create(url)
                connection = client?.connect()
            } catch (e: Exception) {
                // In a real app we might want to fail, but for tests/dev we can just log
                println("Failed to connect to Redis: ${e.message}")
            }
        }
    }

    fun sync(): RedisCommands<String, String> = connection?.sync() ?: throw IllegalStateException("Redis not initialized")

    fun close() {
        connection?.close()
        client?.shutdown()
    }
}
