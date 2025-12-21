package com.fitness.routes

import com.fitness.models.*
import com.fitness.plugins.UserPrincipal
import com.fitness.services.AvailabilityService
import com.fitness.services.ReservationService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.time.LocalDate

fun Route.reservationRoutes() {
    route("/reservations") {
        // Public route to get available slots
        get("/available/{date}") {
            val dateStr = call.parameters["date"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Date required"))

            val date = try {
                LocalDate.parse(dateStr)
            } catch (e: Exception) {
                return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid date format"))
            }

            val slots = AvailabilityService.getAvailableSlots(date)
            call.respond(HttpStatusCode.OK, slots)
        }

        authenticate("auth-jwt") {
            // Create reservation
            post {
                val principal = call.principal<UserPrincipal>()!!
                val request = call.receive<CreateReservationRequest>()
                val reservation = ReservationService.createReservation(principal.userId, request)
                call.respond(HttpStatusCode.Created, reservation)
            }

            // Get my reservations
            get {
                val principal = call.principal<UserPrincipal>()!!
                val reservations = ReservationService.getUserReservations(principal.userId)
                call.respond(HttpStatusCode.OK, reservations)
            }

            // Get my upcoming reservations
            get("/upcoming") {
                val principal = call.principal<UserPrincipal>()!!
                val reservations = ReservationService.getUpcomingReservations(principal.userId)
                call.respond(HttpStatusCode.OK, reservations)
            }

            // Get single reservation
            get("/{id}") {
                val principal = call.principal<UserPrincipal>()!!
                val id = call.parameters["id"]
                    ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID required"))

                val reservation = ReservationService.getReservationById(id)

                // Check if user owns this reservation or is admin
                if (reservation.userId != principal.userId && principal.role != "admin") {
                    return@get call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Access denied"))
                }

                call.respond(HttpStatusCode.OK, reservation)
            }

            // Cancel reservation
            delete("/{id}") {
                val principal = call.principal<UserPrincipal>()!!
                val id = call.parameters["id"]
                    ?: return@delete call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID required"))

                val reservation = ReservationService.cancelReservation(principal.userId, id)
                call.respond(HttpStatusCode.OK, reservation)
            }
        }
    }
}
