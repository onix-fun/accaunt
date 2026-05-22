package profile.auth

import profile.users.UserRepository
import profile.users.User
import profile.infrastructure.security.TokenHasher
import profile.users.toPublicDto
import profile.sessions.SessionRepository
import profile.sessions.Session
import profile.infrastructure.jwt.JwtIssuer
import profile.infrastructure.security.PasswordHasher
import profile.infrastructure.events.EventPublisher
import profile.users.UserSearchService
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*
import io.ktor.server.config.*

class AuthService(
    private val userRepository: UserRepository,
    private val sessionRepository: SessionRepository,
    private val userSearchService: UserSearchService,
    private val jwtIssuer: JwtIssuer,
    private val config: ApplicationConfig
) {
    private val refreshTokenExpDays = config.property("identity.session.refresh_token_exp_days").getString().toLong()

    fun register(email: String, username: String, password: String, firstName: String? = null, lastName: String? = null): User {
        val existing = userRepository.findByEmail(email)
        if (existing != null) throw IllegalArgumentException("User already exists")

        val user = User(
            id = UUID.randomUUID().toString(),
            email = email,
            username = username,
            passwordHash = PasswordHasher.hash(password),
            firstName = firstName,
            lastName = lastName
        )
        userRepository.create(user)
        userSearchService.indexUser(user)
        EventPublisher.publish("user.created", user.id)
        return user
    }

    fun login(email: String, password: String, deviceId: String?, userAgent: String?, ipAddress: String?): LoginResult {
        val user = userRepository.findByEmail(email) ?: throw IllegalArgumentException("Invalid credentials")
        if (!PasswordHasher.verify(user.passwordHash, password)) throw IllegalArgumentException("Invalid credentials")

        val refreshToken = UUID.randomUUID().toString()
        val refreshTokenHash = TokenHasher.hash(refreshToken) // Using deterministic SHA-256 for lookup
        
        val session = Session(
            id = UUID.randomUUID().toString(),
            userId = user.id,
            refreshTokenHash = refreshTokenHash,
            deviceId = deviceId,
            userAgent = userAgent,
            ipAddress = ipAddress,
            expiresAt = Instant.now().plus(refreshTokenExpDays, ChronoUnit.DAYS)
        )
        sessionRepository.create(session)
        
        val accessToken = jwtIssuer.createToken(user.id, session.id, user.role)
        
        return LoginResult(accessToken, refreshToken, session.id, user)
    }

    fun refresh(refreshToken: String): RefreshResult {
        val refreshTokenHash = TokenHasher.hash(refreshToken)
        val session = sessionRepository.findByTokenHash(refreshTokenHash) 
            ?: throw IllegalArgumentException("Invalid refresh token")

        if (session.revokedAt != null) throw IllegalArgumentException("Session revoked")
        if (session.expiresAt.isBefore(Instant.now())) throw IllegalArgumentException("Session expired")
        
        val user = userRepository.findById(session.userId) ?: throw IllegalArgumentException("User not found")
        
        // Reset expiration
        val newExpiresAt = Instant.now().plus(refreshTokenExpDays, ChronoUnit.DAYS)
        sessionRepository.updateExpiration(session.id, newExpiresAt)
        
        val accessToken = jwtIssuer.createToken(user.id, session.id, user.role)
        
        return RefreshResult(accessToken, session.id)
    }

    fun logout(refreshToken: String) {
        val refreshTokenHash = TokenHasher.hash(refreshToken)
        val session = sessionRepository.findByTokenHash(refreshTokenHash) ?: return
        sessionRepository.revoke(session.id)
    }

    fun logoutAll(userId: String) {
        sessionRepository.revokeAllForUser(userId)
    }

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
}

data class LoginResult(
    val accessToken: String,
    val refreshToken: String,
    val sessionId: String,
    val user: profile.users.User
)

data class RefreshResult(
    val accessToken: String,
    val sessionId: String
)
