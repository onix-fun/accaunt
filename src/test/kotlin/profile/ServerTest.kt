package profile

import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.config.*
import io.ktor.server.testing.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import profile.auth.LoginRequest
import profile.auth.RegisterRequest
import kotlin.test.*

class ServerTest {

    private fun TestApplicationBuilder.setupTestConfig() {
        environment {
            config = MapApplicationConfig(
                "identity.jwt.issuer" to "identity-service",
                "identity.jwt.audience" to "gateway",
                "identity.jwt.secret" to "test-secret",
                "identity.jwt.access_token_exp_minutes" to "15",
                "identity.session.refresh_token_exp_days" to "30",
                "postgres.url" to "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
                "postgres.user" to "sa",
                "postgres.password" to "",
                "redis.url" to "redis://localhost:6379",
                "smtp.host" to "localhost",
                "smtp.port" to "2500",
                "s3.endpoint" to "http://localhost:9000",
                "s3.access_key" to "minio",
                "s3.secret_key" to "minio",
                "s3.bucket" to "avatars"
            )
        }
    }

    @Test
    fun `test health endpoint`() = testApplication {
        setupTestConfig()
        application {
            module()
        }
        val response = client.get("/health")
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("UP"))
    }

    @Test
    fun `test registration flow`() = testApplication {
        setupTestConfig()
        application {
            module()
        }
        val registerResponse = client.post("/api/auth/register") {
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            setBody(Json.encodeToString(RegisterRequest("test@example.com", "testuser", "password123")))
        }
        
        assertEquals(HttpStatusCode.Created, registerResponse.status, "Registration failed: ${registerResponse.bodyAsText()}")
        val body = Json.parseToJsonElement(registerResponse.bodyAsText()).jsonObject
        assertEquals("test@example.com", body["email"]?.toString()?.replace("\"", ""))
    }

    @Test
    fun `test login and session listing`() = testApplication {
        setupTestConfig()
        application {
            module()
        }
        
        val client = createClient {
            install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
                json()
            }
        }

        // 1. Register
        val registerResponse = client.post("/api/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest("login@example.com", "loginuser", "password123"))
        }
        assertEquals(HttpStatusCode.Created, registerResponse.status, "Registration failed: ${registerResponse.bodyAsText()}")

        // 2. Login
        val loginResponse = client.post("/api/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest("login@example.com", "password123", "test-device"))
        }

        assertEquals(HttpStatusCode.OK, loginResponse.status, "Login failed: ${loginResponse.bodyAsText()}")
        val loginBody = Json.parseToJsonElement(loginResponse.bodyAsText()).jsonObject
        val userId = loginBody["userId"]?.toString()?.replace("\"", "")
        assertNotNull(userId)

        // Check for refresh_token cookie
        val cookie = loginResponse.headers.getAll(HttpHeaders.SetCookie)?.find { it.startsWith("refresh_token=") }
        assertNotNull(cookie, "Refresh token cookie not found in response")
        assertTrue(cookie.contains("HttpOnly"), "Cookie should be HttpOnly")

        // 3. Get Sessions (using X-User-Id and the cookie is not needed for DB lookup but can be sent)
        val sessionsResponse = client.get("/api/sessions") {
            header("X-User-Id", userId)
            header(HttpHeaders.Cookie, cookie.substringBefore(";"))
        }

        assertEquals(HttpStatusCode.OK, sessionsResponse.status)
        assertTrue(sessionsResponse.bodyAsText().contains("loginuser"), "Sessions list should contain the user")
        assertTrue(sessionsResponse.bodyAsText().contains("test-device"), "Sessions list should contain the device ID")

        // 4. Refresh
        val refreshResponse = client.post("/api/auth/refresh") {
            header(HttpHeaders.Cookie, cookie.substringBefore(";"))
        }
        assertEquals(HttpStatusCode.OK, refreshResponse.status)
        val refreshBody = Json.parseToJsonElement(refreshResponse.bodyAsText()).jsonObject
        assertNotNull(refreshBody["accessToken"])
    }
}
