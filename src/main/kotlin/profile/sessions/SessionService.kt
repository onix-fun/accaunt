package profile.sessions

import profile.infrastructure.db.SessionRepository
import profile.infrastructure.db.UserRepository
import profile.users.toPublicDto

class SessionService(
    private val sessionRepository: SessionRepository,
    private val userRepository: UserRepository
) {
    fun getSessionsForUser(userId: String): List<SessionInfoDto> {
        val sessions = sessionRepository.findActiveByUserId(userId)
        
        return sessions.map { session ->
            val user = userRepository.findById(session.userId)
            SessionInfoDto(
                sessionId = session.id,
                userId = session.userId,
                deviceId = session.deviceId,
                userAgent = session.userAgent,
                lastUsedAt = session.lastUsedAt.toString(),
                user = user?.toPublicDto()
            )
        }
    }

    fun revokeSession(userId: String, sessionId: String) {
        val session = sessionRepository.findById(sessionId) ?: throw IllegalArgumentException("Session not found")
        if (session.userId != userId) throw IllegalArgumentException("Unauthorized to revoke this session")
        sessionRepository.revoke(sessionId)
    }

    fun revokeAllSessions(userId: String) {
        sessionRepository.revokeAllForUser(userId)
    }
}
