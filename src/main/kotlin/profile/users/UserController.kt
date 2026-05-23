package profile.users

import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import profile.infrastructure.db.User

class UserController(
    private val userService: UserService
) {
    suspend fun getMe(call: ApplicationCall) {
        val userId = call.request.headers["X-User-Id"]
        if (userId == null) {
            call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Missing user header from gateway"))
            return
        }

        val user = userService.getProfile(userId)
        if (user == null) {
            call.respond(HttpStatusCode.NotFound, mapOf("error" to "User not found"))
            return
        }

        call.respond(HttpStatusCode.OK, user)
    }

    suspend fun updateMe(call: ApplicationCall) {
        val userId = call.request.headers["X-User-Id"] ?: throw IllegalArgumentException("Unauthorized")
        val request = call.receive<UpdateProfileRequest>()
        
        val updatedUser = userService.updateProfile(userId, request)
        call.respond(HttpStatusCode.OK, updatedUser)
    }

    suspend fun uploadAvatar(call: ApplicationCall) {
        val userId = call.request.headers["X-User-Id"] ?: throw IllegalArgumentException("Unauthorized")
        
        val multipart = call.receiveMultipart()
        var user: User? = null
        
        multipart.forEachPart { part ->
            if (part is PartData.FileItem) {
                val contentType = part.contentType?.toString() ?: "image/jpeg"
                val fileBytes = part.streamProvider().use { it.readBytes() }
                
                user = userService.updateAvatar(userId, fileBytes.inputStream(), contentType)
            }
            part.dispose()
        }
        
        if (user != null) {
            call.respond(HttpStatusCode.OK, user!!)
        } else {
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "No file uploaded"))
        }
    }

    suspend fun getById(call: ApplicationCall) {
        val id = call.parameters["id"] ?: return call.respond(HttpStatusCode.BadRequest, "Missing ID")
        
        try {
            java.util.UUID.fromString(id)
        } catch (e: Exception) {
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid UUID format"))
            return
        }

        val user = userService.getProfile(id)
        if (user == null) {
            call.respond(HttpStatusCode.NotFound, mapOf("error" to "User not found"))
            return
        }

        call.respond(HttpStatusCode.OK, user.toPublicDto())
    }
}
