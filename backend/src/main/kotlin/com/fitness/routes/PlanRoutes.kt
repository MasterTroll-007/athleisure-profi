package com.fitness.routes

import com.fitness.models.*
import com.fitness.plugins.UserPrincipal
import com.fitness.services.PlanService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.planRoutes() {
    route("/plans") {
        // Public routes
        get {
            val plans = PlanService.getPlans()
            call.respond(HttpStatusCode.OK, plans)
        }

        get("/{id}") {
            val id = call.parameters["id"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID required"))

            val plan = PlanService.getPlanById(id)
            call.respond(HttpStatusCode.OK, plan)
        }

        authenticate("auth-jwt") {
            // Purchase a plan
            post("/{id}/purchase") {
                val principal = call.principal<UserPrincipal>()!!
                val id = call.parameters["id"]
                    ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID required"))

                val purchase = PlanService.purchasePlan(principal.userId, id)
                call.respond(HttpStatusCode.Created, purchase)
            }

            // Get my purchased plans
            get("/my") {
                val principal = call.principal<UserPrincipal>()!!
                val plans = PlanService.getMyPlans(principal.userId)
                call.respond(HttpStatusCode.OK, plans)
            }

            // Download plan PDF
            get("/{id}/download") {
                val principal = call.principal<UserPrincipal>()!!
                val id = call.parameters["id"]
                    ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID required"))

                val file = PlanService.getPlanFile(principal.userId, id)
                call.response.header(
                    HttpHeaders.ContentDisposition,
                    ContentDisposition.Attachment.withParameter(
                        ContentDisposition.Parameters.FileName,
                        file.name
                    ).toString()
                )
                call.respondFile(file)
            }

            // Check if plan is purchased
            get("/{id}/check-purchase") {
                val principal = call.principal<UserPrincipal>()!!
                val id = call.parameters["id"]
                    ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID required"))

                val purchased = PlanService.canDownloadPlan(principal.userId, id)
                call.respond(HttpStatusCode.OK, mapOf("purchased" to purchased))
            }
        }
    }
}
