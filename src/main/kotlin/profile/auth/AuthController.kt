package profile.auth

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*

class AuthController(private val authService: AuthService) {

    suspend fun register(call: ApplicationCall) {
        val request = call.receive<RegisterRequest>()
        val user = authService.register(
            request.email, 
            request.username, 
            request.password,
            request.firstName,
            request.lastName
        )
        call.respond(HttpStatusCode.Created, user)
    }

    suspend fun login(call: ApplicationCall) {
        val request = call.receive<LoginRequest>()
        val userAgent = call.request.headers["User-Agent"]
        val ipAddress = call.request.local.remoteHost
        
        val result = authService.login(request.email, request.password, request.deviceId, userAgent, ipAddress)
        
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
    }

    suspend fun verifyEmail(call: ApplicationCall) {
        val userId = call.request.headers["X-User-Id"] ?: throw IllegalArgumentException("Unauthorized")
        val request = call.receive<VerifyEmailRequest>()
        authService.verifyEmail(userId, request.code)
        call.respond(HttpStatusCode.OK, mapOf("message" to "Email verified successfully"))
    }

    suspend fun resendVerification(call: ApplicationCall) {
        val userId = call.request.headers["X-User-Id"] ?: throw IllegalArgumentException("Unauthorized")
        authService.requestEmailVerification(userId)
        call.respond(HttpStatusCode.OK, mapOf("message" to "Verification code resent"))
    }

    suspend fun forgotPassword(call: ApplicationCall) {
        val request = call.receive<ForgotPasswordRequest>()
        authService.forgotPassword(request.email)
        call.respond(HttpStatusCode.OK, mapOf("message" to "If the email exists, a reset link has been sent"))
    }

    suspend fun resetPassword(call: ApplicationCall) {
        val request = call.receive<ResetPasswordRequest>()
        authService.resetPassword(request.token, request.newPassword)
        call.respond(HttpStatusCode.OK, mapOf("message" to "Password has been reset successfully"))
    }

    suspend fun refresh(call: ApplicationCall) {
        val refreshToken = call.request.cookies["refresh_token"]
        
        if (refreshToken == null) {
            call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Missing refresh token cookie"))
            return
        }

        val result = authService.refresh(refreshToken)
        
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
    }

    suspend fun logout(call: ApplicationCall) {
        val refreshToken = call.request.cookies["refresh_token"]
        if (refreshToken != null) {
            authService.logout(refreshToken)
        }
        
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

    suspend fun logoutAll(call: ApplicationCall) {
        val userId = call.request.headers["X-User-Id"]
        if (userId == null) {
            call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Unauthorized"))
            return
        }
        
        authService.logoutAll(userId)
        
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
