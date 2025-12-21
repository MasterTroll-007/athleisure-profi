package com.fitness.routes

import com.fitness.services.GopayService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

fun Route.gopayWebhook() {
    route("/gopay") {
        /**
         * GoPay Webhook endpoint
         *
         * NOTE: This is a placeholder for GoPay webhook handling.
         * Real implementation will need:
         * 1. Signature verification using GoPay secret
         * 2. Proper parsing of GoPay notification format
         * 3. Error handling and retry logic
         */
        post("/notify") {
            try {
                val notification = call.receive<GopayNotification>()

                // TODO: Verify webhook signature
                // val signature = call.request.headers["X-Signature"]
                // if (!verifySignature(signature, requestBody)) {
                //     return@post call.respond(HttpStatusCode.Unauthorized)
                // }

                val success = GopayService.handleNotification(
                    gopayId = notification.id,
                    state = notification.state
                )

                if (success) {
                    call.respond(HttpStatusCode.OK)
                } else {
                    call.respond(HttpStatusCode.NotFound)
                }
            } catch (e: Exception) {
                call.application.log.error("GoPay webhook error", e)
                call.respond(HttpStatusCode.InternalServerError)
            }
        }

        /**
         * Return URL after payment
         * User is redirected here after completing payment on GoPay
         */
        get("/return") {
            val id = call.request.queryParameters["id"]

            // In a real app, this would redirect to the frontend
            // with appropriate success/failure message
            call.respondRedirect("/credits?payment=$id")
        }

        /**
         * Notification URL for GoPay async notifications
         * Same as /notify but via GET for compatibility
         */
        get("/notify") {
            val id = call.request.queryParameters["id"]?.toLongOrNull()
            val state = call.request.queryParameters["state"] ?: "UNKNOWN"

            if (id == null) {
                return@get call.respond(HttpStatusCode.BadRequest)
            }

            val success = GopayService.handleNotification(id, state)

            if (success) {
                call.respond(HttpStatusCode.OK)
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }
    }
}

@Serializable
data class GopayNotification(
    val id: Long,
    val state: String
)
