package profile.auth

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import profile.infrastructure.config.JwtConfig
import profile.users.toProfileDto
import java.security.SecureRandom
import java.util.Base64
import java.util.UUID

class AuthController(
    private val authService: AuthService,
    private val sessionConfig: profile.infrastructure.config.SessionConfig,
    private val jwtConfig: JwtConfig
) {
    private val random = SecureRandom()

    suspend fun register(call: ApplicationCall) {
        val request = call.receive<RegisterRequest>()
        val response = authService.register(
            request.email,
            request.username,
            request.password,
            request.firstName,
            request.lastName
        )
        call.respond(HttpStatusCode.Accepted, response)
    }

    suspend fun confirmRegistration(call: ApplicationCall) {
        val request = call.receive<ConfirmRegistrationRequest>()
        val userAgent = call.request.headers["User-Agent"]
        val ipAddress = call.clientIpAddress()
        val result =
            authService.confirmRegistration(request.email, request.code, request.deviceId, userAgent, ipAddress)
        appendBrowserSession(call, result)
        call.respond(HttpStatusCode.Created, BrowserAuthResponse(result.user.toProfileDto()))
    }

    suspend fun resendRegistrationCode(call: ApplicationCall) {
        val request = call.receive<ResendRegistrationCodeRequest>()
        val response = authService.resendRegistrationCode(request.email)
        call.respond(HttpStatusCode.OK, response)
    }

    suspend fun login(call: ApplicationCall) {
        val request = call.receive<LoginRequest>()
        val userAgent = call.request.headers["User-Agent"]
        val ipAddress = call.clientIpAddress()

        val result = authService.login(
            request.identifier ?: request.email.orEmpty(),
            request.password,
            request.deviceId,
            userAgent,
            ipAddress
        )

        appendBrowserSession(call, result)
        call.respond(HttpStatusCode.OK, BrowserAuthResponse(result.user.toProfileDto()))
    }

    suspend fun token(call: ApplicationCall) {
        val request = call.receive<LoginRequest>()
        val result = authService.login(
            request.identifier ?: request.email.orEmpty(),
            request.password,
            request.deviceId,
            call.request.headers["User-Agent"],
            call.clientIpAddress()
        )
        call.respond(HttpStatusCode.OK, apiTokenResponse(result.accessToken, result.refreshToken))
    }

    suspend fun tokenRefresh(call: ApplicationCall) {
        val request = call.receive<TokenRefreshRequest>()
        val result = authService.refresh(request.refreshToken)
        call.respond(HttpStatusCode.OK, apiTokenResponse(result.accessToken, result.refreshToken))
    }

    suspend fun csrf(call: ApplicationCall) {
        val token = generateOpaqueToken()
        call.response.cookies.append(csrfCookie(token))
        call.respond(HttpStatusCode.OK, CsrfResponse(token))
    }

    suspend fun usernameAvailable(call: ApplicationCall) {
        val username = call.request.queryParameters["username"].orEmpty()
        call.respond(HttpStatusCode.OK, UsernameAvailabilityResponse(authService.isUsernameAvailable(username)))
    }

    suspend fun accounts(call: ApplicationCall) {
        val accounts = requestRefreshTokens(call)
            .mapNotNull { (userId, token) ->
                authService.accountForRefreshToken(token)?.takeIf { it.id == userId }
            }
            .distinctBy { it.id }
            .map { it.toBrowserAccountDto() }
        call.respond(HttpStatusCode.OK, accounts)
    }

    suspend fun switchAccount(call: ApplicationCall) {
        val request = call.receive<SwitchAccountRequest>()
        val userId = requireUserId(request.userId)
        val refreshToken = call.request.cookies[getRefreshCookieName(userId)]
            ?: throw IllegalArgumentException("Account session not found")
        val result = refreshBrowserAccount(refreshToken, userId)
        appendBrowserRefresh(call, result)
        call.respond(HttpStatusCode.OK, BrowserAuthResponse(result.user.toProfileDto()))
    }

    suspend fun verifyEmail(call: ApplicationCall) {
        val userId = call.principal<JWTPrincipal>()!!.payload.subject
        val request = call.receive<VerifyEmailRequest>()
        authService.verifyEmail(userId, request.code)
        call.respond(HttpStatusCode.OK, mapOf("message" to "Email verified successfully"))
    }

    suspend fun resendVerification(call: ApplicationCall) {
        val userId = call.principal<JWTPrincipal>()!!.payload.subject
        authService.requestEmailVerification(userId)
        call.respond(HttpStatusCode.OK, mapOf("message" to "Verification code resent"))
    }

    suspend fun forgotPassword(call: ApplicationCall) {
        val request = call.receive<ForgotPasswordRequest>()
        authService.forgotPassword(request.identifier ?: request.email.orEmpty())
        call.respond(HttpStatusCode.OK, mapOf("message" to "If the account exists, a reset code has been sent"))
    }

    suspend fun resetPassword(call: ApplicationCall) {
        val request = call.receive<ResetPasswordRequest>()
        authService.resetPassword(
            request.identifier ?: request.email.orEmpty(),
            request.code ?: request.token.orEmpty(),
            request.newPassword
        )
        call.respond(HttpStatusCode.OK, mapOf("message" to "Password has been reset successfully"))
    }

    suspend fun refresh(call: ApplicationCall) {
        val activeUserId = call.request.cookies[ACTIVE_USER_COOKIE_NAME]?.let(::parseUserId)
        val refreshToken = activeUserId?.let { call.request.cookies[getRefreshCookieName(it)] }

        if (refreshToken == null) {
            call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Missing refresh token cookie"))
            return
        }

        val result = refreshBrowserAccount(refreshToken, activeUserId)
        appendBrowserRefresh(call, result)
        call.respond(HttpStatusCode.OK, BrowserAuthResponse(result.user.toProfileDto()))
    }

    suspend fun logout(call: ApplicationCall) {
        val userId = call.request.cookies[ACTIVE_USER_COOKIE_NAME]?.let(::parseUserId)
        val refreshToken = userId?.let { call.request.cookies[getRefreshCookieName(it)] }
        if (refreshToken != null) {
            authService.logout(refreshToken)
        }

        if (userId != null) call.response.cookies.append(clearRefreshCookie(userId))
        clearActiveBrowserSession(call)

        call.respond(HttpStatusCode.OK, mapOf("message" to "Logged out"))
    }

    suspend fun logoutAll(call: ApplicationCall) {
        val userId = call.principal<JWTPrincipal>()?.payload?.subject
        if (userId == null) {
            call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Unauthorized"))
            return
        }

        authService.logoutAll(userId)

        requestRefreshTokens(call)
            .keys
            .filter { it == userId }
            .forEach { call.response.cookies.append(clearRefreshCookie(it)) }
        clearActiveBrowserSession(call)

        call.respond(HttpStatusCode.OK, mapOf("message" to "Logged out from all devices"))
    }

    private fun appendBrowserSession(call: ApplicationCall, result: LoginResult) {
        call.response.cookies.append(refreshCookie(result.user.id, result.refreshToken))
        call.response.cookies.append(accessCookie(result.accessToken))
        call.response.cookies.append(activeUserCookie(result.user.id))
    }

    private fun appendBrowserRefresh(call: ApplicationCall, result: RefreshResult) {
        call.response.cookies.append(refreshCookie(result.user.id, result.refreshToken))
        call.response.cookies.append(accessCookie(result.accessToken))
        call.response.cookies.append(activeUserCookie(result.user.id))
    }

    private fun clearActiveBrowserSession(call: ApplicationCall) {
        call.response.cookies.append(clearAccessCookie())
        call.response.cookies.append(clearActiveUserCookie())
    }

    private fun getRefreshCookieName(userId: String): String {
        return "${REFRESH_COOKIE_PREFIX}$userId"
    }

    private fun refreshBrowserAccount(refreshToken: String, userId: String): RefreshResult {
        val account = authService.accountForRefreshToken(refreshToken)
        if (account?.id != userId) throw IllegalArgumentException("Account session mismatch")
        return authService.refresh(refreshToken)
    }

    private fun requireUserId(value: String): String {
        return parseUserId(value) ?: throw IllegalArgumentException("Invalid account id")
    }

    private fun parseUserId(value: String): String? {
        return runCatching { UUID.fromString(value).toString() }.getOrNull()
    }

    private fun refreshCookie(userId: String, value: String): Cookie {
        return Cookie(
            name = getRefreshCookieName(userId),
            value = value,
            httpOnly = true,
            secure = sessionConfig.cookieSecure,
            domain = sessionConfig.cookieDomain,
            path = REFRESH_COOKIE_PATH,
            maxAge = refreshCookieMaxAgeSeconds(),
            extensions = mapOf("SameSite" to "Strict")
        )
    }

    private fun clearRefreshCookie(userId: String): Cookie {
        return Cookie(
            name = getRefreshCookieName(userId),
            value = "",
            httpOnly = true,
            secure = sessionConfig.cookieSecure,
            domain = sessionConfig.cookieDomain,
            path = REFRESH_COOKIE_PATH,
            maxAge = 0,
            extensions = mapOf("SameSite" to "Strict")
        )
    }

    private fun accessCookie(value: String) = browserCookie(
        name = ACCESS_COOKIE_NAME,
        value = value,
        maxAge = accessCookieMaxAgeSeconds()
    )

    private fun clearAccessCookie() = browserCookie(name = ACCESS_COOKIE_NAME, value = "", maxAge = 0)

    private fun activeUserCookie(userId: String) = browserCookie(
        name = ACTIVE_USER_COOKIE_NAME,
        value = userId,
        maxAge = refreshCookieMaxAgeSeconds()
    )

    private fun clearActiveUserCookie() = browserCookie(name = ACTIVE_USER_COOKIE_NAME, value = "", maxAge = 0)

    private fun csrfCookie(value: String) = browserCookie(name = CSRF_COOKIE_NAME, value = value)

    private fun browserCookie(name: String, value: String, maxAge: Int? = null): Cookie {
        return Cookie(
            name = name,
            value = value,
            httpOnly = true,
            secure = sessionConfig.cookieSecure,
            domain = sessionConfig.cookieDomain,
            path = "/",
            maxAge = maxAge,
            extensions = mapOf("SameSite" to "Strict")
        )
    }

    private fun refreshCookieMaxAgeSeconds(): Int {
        return (sessionConfig.refreshTokenExpDays * 24 * 60 * 60)
            .coerceAtMost(Int.MAX_VALUE.toLong())
            .toInt()
    }

    private fun accessCookieMaxAgeSeconds(): Int {
        return (jwtConfig.accessTokenExpMinutes * 60)
            .coerceAtMost(Int.MAX_VALUE.toLong())
            .toInt()
    }

    private fun requestRefreshTokens(call: ApplicationCall): Map<String, String> {
        return call.request.cookies.rawCookies
            .filterKeys { it.startsWith(REFRESH_COOKIE_PREFIX) }
            .mapNotNull { (name, value) ->
                parseUserId(name.removePrefix(REFRESH_COOKIE_PREFIX))?.let { it to value }
            }
            .toMap()
    }

    private fun apiTokenResponse(accessToken: String, refreshToken: String) = ApiTokenResponse(
        accessToken = accessToken,
        refreshToken = refreshToken,
        expiresIn = jwtConfig.accessTokenExpMinutes * 60
    )

    private fun generateOpaqueToken(): String {
        val bytes = ByteArray(32)
        random.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    private fun ApplicationCall.clientIpAddress(): String {
        return request.headers["X-Forwarded-For"]
            ?.substringBefore(",")
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: request.headers["X-Real-IP"]
            ?: request.local.remoteHost
    }

    private companion object {
        private const val REFRESH_COOKIE_PREFIX = "refresh_token_"
        private const val REFRESH_COOKIE_PATH = "/api/auth"
        private const val ACCESS_COOKIE_NAME = "access_token"
        private const val ACTIVE_USER_COOKIE_NAME = "active_user"
        private const val CSRF_COOKIE_NAME = "csrf_token"
    }
}
