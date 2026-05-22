package profile

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import io.ktor.server.response.*
import io.ktor.http.*
import io.ktor.server.plugins.openapi.*
import profile.auth.AuthService
import profile.auth.authRouting
import profile.infrastructure.db.DatabaseFactory
import profile.infrastructure.jwt.JwtIssuer
import profile.infrastructure.redis.RedisManager
import profile.sessions.SessionRepository
import profile.users.UserRepository
import profile.users.UserSearchService
import profile.users.userRouting

fun Application.module() {
    // 1. Initialize Infrastructure
    DatabaseFactory.init(environment.config)
    RedisManager.init(environment.config)
    
    val jwtIssuer = JwtIssuer(environment.config)
    
    // 2. Initialize Repositories
    val userRepository = UserRepository()
    val sessionRepository = SessionRepository()
    
    // 3. Initialize Services
    val userSearchService = UserSearchService(userRepository)
    userSearchService.indexAllUsers()
    val authService = AuthService(userRepository, sessionRepository, userSearchService, jwtIssuer, environment.config)
    
    // 4. Configure Plugins
    install(ContentNegotiation) {
        json()
    }
    
    // 5. Configure Routing
    routing {
        // In Ktor 3.0, the openAPI parameter might have changed. 
        // Let's use the version that works with the current classpath.
        // Actually the error says: fun Route.openAPI(path: String, swaggerFile: String, ...)
        openAPI(path = "openapi", swaggerFile = "openapi.yaml")

        get("/health") {
            call.respond(HttpStatusCode.OK, mapOf("status" to "UP"))
        }
        
        authRouting(authService)
        userRouting(userRepository, userSearchService)
    }
}
