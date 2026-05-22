package profile.users

import java.time.Instant
import java.util.*
import kotlinx.serialization.Serializable
import profile.shared.InstantSerializer

@Serializable
data class User(
    val id: String,
    val email: String,
    val username: String,
    val passwordHash: String,
    val emailVerified: Boolean = false,
    val firstName: String? = null,
    val lastName: String? = null,
    val displayName: String? = null,
    val avatarUrl: String? = null,
    val bio: String? = null,
    val role: String = "USER",
    val status: String = "ACTIVE",
    @Serializable(with = InstantSerializer::class)
    val createdAt: Instant = Instant.now(),
    @Serializable(with = InstantSerializer::class)
    val updatedAt: Instant = Instant.now()
)
