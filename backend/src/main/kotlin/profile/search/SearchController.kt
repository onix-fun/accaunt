package profile.search

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import profile.users.UserPublicDto
import profile.shared.ApiErrorCode
import profile.shared.apiError

class SearchController(private val searchService: SearchService) {

    suspend fun search(call: ApplicationCall) {
        val query = call.request.queryParameters["q"]
        if (query.isNullOrBlank()) {
            call.respond(HttpStatusCode.OK, emptyList<UserPublicDto>())
            return
        }

        if (query.length < 2) {
            apiError(ApiErrorCode.VALIDATION_SEARCH_QUERY_TOO_SHORT, "q")
        }

        val results = searchService.searchByUsernamePrefix(query)
        call.respond(HttpStatusCode.OK, results)
    }
}
