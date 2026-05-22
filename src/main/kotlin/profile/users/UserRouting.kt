package profile.users

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.userRouting(
    userRepository: UserRepository,
    userSearchService: UserSearchService
) {
    route("/api/users") {
        get("/me") {
            val userId = call.request.headers["X-User-Id"]
            if (userId == null) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Missing user header from gateway"))
                return@get
            }

            val user = userRepository.findById(userId)
            if (user == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "User not found"))
                return@get
            }

            call.respond(HttpStatusCode.OK, user)
        }

        // Public: Find by ID
        get("/{id}") {
            val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing ID")
            
            // UUID Validation
            try {
                java.util.UUID.fromString(id)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid UUID format"))
                return@get
            }

            val user = userRepository.findById(id)
            if (user == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "User not found"))
                return@get
            }

            call.respond(HttpStatusCode.OK, user.toPublicDto())
        }

        // Public: Find ID by username
        get("/id-by-username/{username}") {
            val username = call.parameters["username"] ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing username")
            val userId = userSearchService.getUserIdByUsername(username)
            
            if (userId == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "User not found"))
                return@get
            }

            call.respond(HttpStatusCode.OK, mapOf("id" to userId))
        }

        // Public: Full-text search by username
        get("/search") {
            val query = call.request.queryParameters["q"]
            if (query.isNullOrBlank()) {
                call.respond(HttpStatusCode.OK, emptyList<UserPublicDto>())
                return@get
            }

            if (query.length < 2) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Query must be at least 2 characters long"))
                return@get
            }

            val results = userSearchService.searchByUsernamePrefix(query)
            call.respond(HttpStatusCode.OK, results)
        }
    }
}
