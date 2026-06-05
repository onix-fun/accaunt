package profile.infrastructure.jwt

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.config.*
import java.util.*

class JwtIssuer(config: ApplicationConfig) {
    private val issuer = config.property("identity.jwt.issuer").getString()
    private val audience = config.property("identity.jwt.audience").getString()
    private val validityInMinutes = config.property("identity.jwt.access_token_exp_minutes").getString().toLong()
    private val algorithm = Algorithm.RSA256(
        RsaKeyLoader.loadPublicKey(config.property("identity.jwt.public_key_path").getString()),
        RsaKeyLoader.loadPrivateKey(config.property("identity.jwt.private_key_path").getString())
    )

    fun createToken(userId: String, sessionId: String, role: String): String {
        return JWT.create()
            .withAudience(audience)
            .withIssuer(issuer)
            .withSubject(userId)
            .withClaim("sid", sessionId)
            .withClaim("role", role)
            .withExpiresAt(Date(System.currentTimeMillis() + validityInMinutes * 60 * 1000))
            .sign(algorithm)
    }
}
