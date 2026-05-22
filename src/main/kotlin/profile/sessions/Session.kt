package profile.sessions

import java.time.Instant
import kotlinx.serialization.Serializable
import profile.shared.InstantSerializer

@Serializable
data class Session(
    val id: String,
    val userId: String,
    val refreshTokenHash: String,
    val deviceId: String? = null,
    val userAgent: String? = null,
    val ipAddress: String? = null,
    @Serializable(with = InstantSerializer::class)
    val expiresAt: Instant,
    @Serializable(with = InstantSerializer::class)
    val lastUsedAt: Instant = Instant.now(),
    @Serializable(with = InstantSerializer::class)
    val revokedAt: Instant? = null,
    @Serializable(with = InstantSerializer::class)
    val createdAt: Instant = Instant.now()
)
