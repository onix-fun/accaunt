package profile.sessions

import kotlinx.serialization.Serializable
import profile.users.UserPublicDto

@Serializable
data class SessionInfoDto(
    val sessionId: String,
    val userId: String,
    val deviceId: String? = null,
    val userAgent: String? = null,
    val lastUsedAt: String,
    val user: UserPublicDto? = null
)
