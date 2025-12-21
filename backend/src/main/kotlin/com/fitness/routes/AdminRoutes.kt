package com.fitness.routes

import com.fitness.models.*
import com.fitness.plugins.UserPrincipal
import com.fitness.repositories.AvailabilityBlockRepository
import com.fitness.repositories.UserRepository
import com.fitness.services.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import java.time.LocalDate

fun Route.adminRoutes() {
    authenticate("auth-admin") {
        route("/admin") {
            // Dashboard stats
            get("/dashboard") {
                val today = LocalDate.now()
                val weekStart = today.minusDays(today.dayOfWeek.value.toLong() - 1)
                val weekEnd = weekStart.plusDays(6)

                val todayReservations = ReservationService.getReservationsByDate(today)
                val weekReservations = ReservationService.getReservationsByDateRange(weekStart, weekEnd)

                call.respond(HttpStatusCode.OK, DashboardStats(
                    todayReservations = todayReservations.size,
                    weekReservations = weekReservations.size,
                    todayList = todayReservations
                ))
            }

            // Calendar events
            get("/calendar") {
                val startStr = call.request.queryParameters["start"]
                val endStr = call.request.queryParameters["end"]

                if (startStr == null || endStr == null) {
                    return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Start and end dates required"))
                }

                val startDate = LocalDate.parse(startStr)
                val endDate = LocalDate.parse(endStr)

                val events = ReservationService.getCalendarEvents(startDate, endDate)
                call.respond(HttpStatusCode.OK, events)
            }

            // Availability Blocks
            route("/blocks") {
                get {
                    val blocks = AvailabilityBlockRepository.findAll()
                    call.respond(HttpStatusCode.OK, blocks)
                }

                post {
                    val request = call.receive<CreateAvailabilityBlockRequest>()
                    val block = AvailabilityBlockRepository.create(request)
                    call.respond(HttpStatusCode.Created, block)
                }

                get("/{id}") {
                    val id = call.parameters["id"]
                        ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID required"))

                    val block = AvailabilityBlockRepository.findById(java.util.UUID.fromString(id))
                        ?: return@get call.respond(HttpStatusCode.NotFound, mapOf("error" to "Block not found"))

                    call.respond(HttpStatusCode.OK, block)
                }

                patch("/{id}") {
                    val id = call.parameters["id"]
                        ?: return@patch call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID required"))

                    val request = call.receive<UpdateAvailabilityBlockRequest>()
                    val block = AvailabilityBlockRepository.update(java.util.UUID.fromString(id), request)
                        ?: return@patch call.respond(HttpStatusCode.NotFound, mapOf("error" to "Block not found"))

                    call.respond(HttpStatusCode.OK, block)
                }

                delete("/{id}") {
                    val id = call.parameters["id"]
                        ?: return@delete call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID required"))

                    AvailabilityBlockRepository.delete(java.util.UUID.fromString(id))
                    call.respond(HttpStatusCode.OK, mapOf("message" to "Block deleted"))
                }
            }

            // Clients
            route("/clients") {
                get {
                    val query = call.request.queryParameters["q"]
                    val clients = if (query != null) {
                        UserRepository.searchClients(query)
                    } else {
                        UserRepository.findAllClients()
                    }
                    call.respond(HttpStatusCode.OK, clients)
                }

                get("/{id}") {
                    val id = call.parameters["id"]
                        ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID required"))

                    val client = UserRepository.findById(java.util.UUID.fromString(id))
                        ?: return@get call.respond(HttpStatusCode.NotFound, mapOf("error" to "Client not found"))

                    call.respond(HttpStatusCode.OK, client)
                }

                get("/{id}/reservations") {
                    val id = call.parameters["id"]
                        ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID required"))

                    val reservations = ReservationService.getUserReservations(id)
                    call.respond(HttpStatusCode.OK, reservations)
                }

                get("/{id}/transactions") {
                    val id = call.parameters["id"]
                        ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID required"))

                    val transactions = CreditService.getTransactionHistory(id, 100)
                    call.respond(HttpStatusCode.OK, transactions)
                }

                get("/{id}/notes") {
                    val id = call.parameters["id"]
                        ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID required"))

                    val notes = PlanService.getClientNotes(id)
                    call.respond(HttpStatusCode.OK, notes)
                }

                post("/{id}/notes") {
                    val principal = call.principal<UserPrincipal>()!!
                    val id = call.parameters["id"]
                        ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID required"))

                    val request = call.receive<CreateClientNoteRequest>()
                    val note = PlanService.createClientNote(principal.userId, request.copy(userId = id))
                    call.respond(HttpStatusCode.Created, note)
                }

                delete("/notes/{noteId}") {
                    val noteId = call.parameters["noteId"]
                        ?: return@delete call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Note ID required"))

                    PlanService.deleteClientNote(noteId)
                    call.respond(HttpStatusCode.OK, mapOf("message" to "Note deleted"))
                }

                post("/{id}/adjust-credits") {
                    val id = call.parameters["id"]
                        ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID required"))

                    val request = call.receive<AdminAdjustCreditsRequest>()
                    val result = CreditService.adjustCredits(id, request.copy(userId = id))
                    call.respond(HttpStatusCode.OK, result)
                }
            }

            // Reservations management
            route("/reservations") {
                get {
                    val reservations = ReservationService.getAllReservations()
                    call.respond(HttpStatusCode.OK, reservations)
                }

                delete("/{id}") {
                    val id = call.parameters["id"]
                        ?: return@delete call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID required"))

                    val refund = call.request.queryParameters["refund"]?.toBoolean() ?: true
                    val reservation = ReservationService.adminCancelReservation(id, refund)
                    call.respond(HttpStatusCode.OK, reservation)
                }
            }

            // Training Plans management
            route("/plans") {
                get {
                    val plans = PlanService.getAllPlans()
                    call.respond(HttpStatusCode.OK, plans)
                }

                post {
                    val request = call.receive<CreateTrainingPlanRequest>()
                    val plan = PlanService.createPlan(request)
                    call.respond(HttpStatusCode.Created, plan)
                }

                patch("/{id}") {
                    val id = call.parameters["id"]
                        ?: return@patch call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID required"))

                    val request = call.receive<UpdateTrainingPlanRequest>()
                    val plan = PlanService.updatePlan(id, request)
                    call.respond(HttpStatusCode.OK, plan)
                }

                delete("/{id}") {
                    val id = call.parameters["id"]
                        ?: return@delete call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID required"))

                    PlanService.deletePlan(id)
                    call.respond(HttpStatusCode.OK, mapOf("message" to "Plan deleted"))
                }

                post("/{id}/upload-pdf") {
                    val id = call.parameters["id"]
                        ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID required"))

                    val multipart = call.receiveMultipart()
                    var uploaded = false

                    multipart.forEachPart { part ->
                        if (part is PartData.FileItem) {
                            val fileName = part.originalFileName ?: "plan.pdf"
                            val fileBytes = part.streamProvider().readBytes()
                            PlanService.uploadPlanFile(id, fileName, fileBytes)
                            uploaded = true
                        }
                        part.dispose()
                    }

                    if (uploaded) {
                        call.respond(HttpStatusCode.OK, mapOf("message" to "PDF uploaded"))
                    } else {
                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to "No file provided"))
                    }
                }

                post("/{id}/upload-preview") {
                    val id = call.parameters["id"]
                        ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID required"))

                    val multipart = call.receiveMultipart()
                    var uploaded = false

                    multipart.forEachPart { part ->
                        if (part is PartData.FileItem) {
                            val fileName = part.originalFileName ?: "preview.jpg"
                            val fileBytes = part.streamProvider().readBytes()
                            PlanService.uploadPlanPreview(id, fileName, fileBytes)
                            uploaded = true
                        }
                        part.dispose()
                    }

                    if (uploaded) {
                        call.respond(HttpStatusCode.OK, mapOf("message" to "Preview uploaded"))
                    } else {
                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to "No file provided"))
                    }
                }
            }

            // Pricing management
            route("/pricing") {
                get {
                    val items = CreditService.getAllPricingItems()
                    call.respond(HttpStatusCode.OK, items)
                }

                post {
                    val request = call.receive<CreatePricingItemRequest>()
                    val item = CreditService.createPricingItem(request)
                    call.respond(HttpStatusCode.Created, item)
                }

                patch("/{id}") {
                    val id = call.parameters["id"]
                        ?: return@patch call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID required"))

                    val request = call.receive<UpdatePricingItemRequest>()
                    val item = CreditService.updatePricingItem(id, request)
                    call.respond(HttpStatusCode.OK, item)
                }

                delete("/{id}") {
                    val id = call.parameters["id"]
                        ?: return@delete call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID required"))

                    CreditService.deletePricingItem(id)
                    call.respond(HttpStatusCode.OK, mapOf("message" to "Pricing item deleted"))
                }
            }

            // Credit packages management
            route("/packages") {
                get {
                    val packages = CreditService.getAllPackages()
                    call.respond(HttpStatusCode.OK, packages)
                }

                post {
                    val request = call.receive<CreateCreditPackageRequest>()
                    val pkg = CreditService.createPackage(request)
                    call.respond(HttpStatusCode.Created, pkg)
                }

                patch("/{id}") {
                    val id = call.parameters["id"]
                        ?: return@patch call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID required"))

                    val request = call.receive<UpdateCreditPackageRequest>()
                    val pkg = CreditService.updatePackage(id, request)
                    call.respond(HttpStatusCode.OK, pkg)
                }

                delete("/{id}") {
                    val id = call.parameters["id"]
                        ?: return@delete call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID required"))

                    CreditService.deletePackage(id)
                    call.respond(HttpStatusCode.OK, mapOf("message" to "Package deleted"))
                }
            }

            // Payments
            route("/payments") {
                get {
                    val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 100
                    val payments = GopayService.getAllPayments(limit)
                    call.respond(HttpStatusCode.OK, payments)
                }
            }

            // Transactions
            route("/transactions") {
                get {
                    val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 100
                    val transactions = CreditService.getAllTransactions(limit)
                    call.respond(HttpStatusCode.OK, transactions)
                }
            }
        }
    }
}

@Serializable
data class DashboardStats(
    val todayReservations: Int,
    val weekReservations: Int,
    val todayList: List<ReservationDTO>
)
