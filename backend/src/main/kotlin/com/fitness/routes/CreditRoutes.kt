package com.fitness.routes

import com.fitness.models.*
import com.fitness.plugins.UserPrincipal
import com.fitness.services.CreditService
import com.fitness.services.GopayService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.creditRoutes() {
    route("/credits") {
        // Public routes
        get("/packages") {
            val packages = CreditService.getPackages()
            call.respond(HttpStatusCode.OK, packages)
        }

        get("/pricing") {
            val items = CreditService.getPricingItems()
            call.respond(HttpStatusCode.OK, items)
        }

        authenticate("auth-jwt") {
            // Get my credit balance
            get("/balance") {
                val principal = call.principal<UserPrincipal>()!!
                val balance = CreditService.getBalance(principal.userId)
                call.respond(HttpStatusCode.OK, balance)
            }

            // Get my transaction history
            get("/history") {
                val principal = call.principal<UserPrincipal>()!!
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 50
                val history = CreditService.getTransactionHistory(principal.userId, limit)
                call.respond(HttpStatusCode.OK, history)
            }

            // Purchase credits (create GoPay payment)
            post("/purchase") {
                val principal = call.principal<UserPrincipal>()!!
                val request = call.receive<CreatePaymentRequest>()
                val payment = GopayService.createPayment(principal.userId, request.packageId)
                call.respond(HttpStatusCode.OK, payment)
            }

            // Get payment status
            get("/payment/{id}") {
                val principal = call.principal<UserPrincipal>()!!
                val id = call.parameters["id"]
                    ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID required"))

                val payment = GopayService.getPaymentStatus(id)

                // Check if user owns this payment or is admin
                if (payment.userId != principal.userId && principal.role != "admin") {
                    return@get call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Access denied"))
                }

                call.respond(HttpStatusCode.OK, payment)
            }

            // Simulate successful payment (for testing)
            post("/payment/{id}/simulate-success") {
                val principal = call.principal<UserPrincipal>()!!
                val id = call.parameters["id"]
                    ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID required"))

                val payment = GopayService.getPaymentStatus(id)

                // Check if user owns this payment
                if (payment.userId != principal.userId && principal.role != "admin") {
                    return@post call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Access denied"))
                }

                val result = GopayService.simulateSuccessfulPayment(id)
                call.respond(HttpStatusCode.OK, result)
            }

            // Get my payments
            get("/payments") {
                val principal = call.principal<UserPrincipal>()!!
                val payments = GopayService.getPaymentsByUser(principal.userId)
                call.respond(HttpStatusCode.OK, payments)
            }
        }
    }
}
