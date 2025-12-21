package com.fitness.plugins

import com.fitness.routes.*
import io.ktor.server.application.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    routing {
        route("/api") {
            authRoutes()
            reservationRoutes()
            creditRoutes()
            planRoutes()
            adminRoutes()
            gopayWebhook()
        }
    }
}
