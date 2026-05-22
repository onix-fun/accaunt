package profile.infrastructure.events

import profile.infrastructure.redis.RedisManager
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

@Serializable
data class DomainEvent(
    val type: String,
    val payload: String,
    val timestamp: Long = System.currentTimeMillis()
)

object EventPublisher {
    fun publish(type: String, payload: String) {
        val event = DomainEvent(type, payload)
        val jsonEvent = Json.encodeToString(event)
        try {
            RedisManager.sync().publish("identity_events", jsonEvent)
        } catch (e: Exception) {
            println("Skipping event publish: ${e.message}")
        }
    }
}
