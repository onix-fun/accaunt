package profile.auth

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.authRouting(authService: AuthService) {
    route("/api/auth") {
        post("/register") {
            val request = call.receive<RegisterRequest>()
            try {
                val user = authService.register(
                    request.email, 
                    request.username, 
                    request.password,
                    request.firstName,
                    request.lastName
                )
                call.respond(HttpStatusCode.Created, user)
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to e.message))
            }
        }

        post("/login") {
            val request = call.receive<LoginRequest>()
            val userAgent = call.request.headers["User-Agent"]
            val ipAddress = call.request.local.remoteHost
            try {
                val result = authService.login(request.email, request.password, request.deviceId, userAgent, ipAddress)
                
                // Set single HttpOnly Cookie refresh_token
                call.response.cookies.append(
                    Cookie(
                        name = "refresh_token",
                        value = result.refreshToken,
                        httpOnly = true,
                        secure = true,
                        path = "/",
                        maxAge = 30 * 24 * 60 * 60
                    )
                )

                call.respond(HttpStatusCode.OK, AuthResponse(result.accessToken, result.user.id, result.user))
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to e.message))
            }
        }

        post("/refresh") {
            val refreshToken = call.request.cookies["refresh_token"]
            
            if (refreshToken == null) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Missing refresh token cookie"))
                return@post
            }

            try {
                val result = authService.refresh(refreshToken)
                
                // Refresh the cookie expiration
                call.response.cookies.append(
                    Cookie(
                        name = "refresh_token",
                        value = refreshToken,
                        httpOnly = true,
                        secure = true,
                        path = "/",
                        maxAge = 30 * 24 * 60 * 60
                    )
                )

                call.respond(HttpStatusCode.OK, mapOf("accessToken" to result.accessToken))
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to e.message))
            }
        }

        get("/sessions") {
            val userId = call.request.headers["X-User-Id"]
            if (userId == null) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Unauthorized"))
                return@get
            }
            
            val sessions = authService.getSessionsForUser(userId)
            call.respond(HttpStatusCode.OK, sessions)
        }

        post("/logout") {
            val refreshToken = call.request.cookies["refresh_token"]
            if (refreshToken != null) {
                authService.logout(refreshToken)
            }
            
            // Delete cookie
            call.response.cookies.append(
                Cookie(
                    name = "refresh_token",
                    value = "",
                    path = "/",
                    maxAge = 0
                )
            )
            
            call.respond(HttpStatusCode.OK, mapOf("message" to "Logged out"))
        }

        post("/logout-all") {
            val userId = call.request.headers["X-User-Id"]
            if (userId == null) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Unauthorized"))
                return@post
            }
            
            authService.logoutAll(userId)
            
            // Also clear the current cookie if any
            call.response.cookies.append(
                Cookie(
                    name = "refresh_token",
                    value = "",
                    path = "/",
                    maxAge = 0
                )
            )

            call.respond(HttpStatusCode.OK, mapOf("message" to "Logged out from all devices"))
        }
    }
}
