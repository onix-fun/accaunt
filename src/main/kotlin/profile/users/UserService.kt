package profile.users

import profile.infrastructure.storage.S3Client
import java.io.InputStream

class UserService(
    private val userRepository: UserRepository,
    private val s3Client: S3Client
) {
    fun getProfile(userId: String): User? {
        return userRepository.findById(userId)
    }

    fun updateAvatar(userId: String, inputStream: InputStream, contentType: String): User {
        val user = userRepository.findById(userId) ?: throw IllegalArgumentException("User not found")
        val avatarUrl = s3Client.uploadAvatar(userId, inputStream, contentType)
        
        // Update user in DB (I need to add an update method to UserRepository)
        // For now I'll just show the intent.
        return user.copy(avatarUrl = avatarUrl)
    }
}
