package profile.users

import kotlinx.serialization.Serializable

@Serializable
data class UserPublicDto(
    val id: String,
    val username: String,
    val firstName: String? = null,
    val lastName: String? = null,
    val avatarUrl: String? = null
)

fun User.toPublicDto() = UserPublicDto(
    id = id,
    username = username,
    firstName = firstName,
    lastName = lastName,
    avatarUrl = avatarUrl
)
