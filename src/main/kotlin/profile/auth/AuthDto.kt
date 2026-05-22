package profile.auth

import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(
    val email: String,
    val username: String,
    val password: String,
    val firstName: String? = null,
    val lastName: String? = null
)

@Serializable
data class LoginRequest(
    val email: String, 
    val password: String,
    val deviceId: String? = null
)

@Serializable
data class AuthResponse(
    val accessToken: String,
    val userId: String,
    val user: profile.users.User
)

// No body needed for Refresh or Logout as they use the single HttpOnly cookie.
// But we might still want the DTOs if we use them in the routes (though they'll be empty).
// Let's remove them if they aren't needed.

@Serializable
data class SessionInfoDto(
    val sessionId: String,
    val userId: String,
    val deviceId: String? = null,
    val userAgent: String? = null,
    val lastUsedAt: String,
    val user: profile.users.UserPublicDto? = null
)
