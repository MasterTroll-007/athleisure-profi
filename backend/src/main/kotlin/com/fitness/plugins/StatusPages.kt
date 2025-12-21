package com.fitness.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import kotlinx.serialization.Serializable

fun Application.configureStatusPages() {
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            when (cause) {
                is IllegalArgumentException -> {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ErrorResponse("Bad Request", cause.message ?: "Invalid request")
                    )
                }
                is IllegalStateException -> {
                    call.respond(
                        HttpStatusCode.Conflict,
                        ErrorResponse("Conflict", cause.message ?: "Operation not allowed")
                    )
                }
                is NoSuchElementException -> {
                    call.respond(
                        HttpStatusCode.NotFound,
                        ErrorResponse("Not Found", cause.message ?: "Resource not found")
                    )
                }
                is SecurityException -> {
                    call.respond(
                        HttpStatusCode.Forbidden,
                        ErrorResponse("Forbidden", cause.message ?: "Access denied")
                    )
                }
                else -> {
                    call.application.log.error("Unhandled exception", cause)
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorResponse("Internal Server Error", "An unexpected error occurred")
                    )
                }
            }
        }

        status(HttpStatusCode.Unauthorized) { call, status ->
            call.respond(status, ErrorResponse("Unauthorized", "Authentication required"))
        }

        status(HttpStatusCode.Forbidden) { call, status ->
            call.respond(status, ErrorResponse("Forbidden", "You don't have permission to access this resource"))
        }
    }
}

@Serializable
data class ErrorResponse(
    val error: String,
    val message: String
)
