package com.fitness.routes

import com.fitness.models.*
import com.fitness.plugins.UserPrincipal
import com.fitness.services.AuthService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.authRoutes() {
    route("/auth") {
        post("/register") {
            val request = call.receive<RegisterRequest>()
            val response = AuthService.register(request)
            call.respond(HttpStatusCode.Created, response)
        }

        post("/login") {
            val request = call.receive<LoginRequest>()
            val response = AuthService.login(request)
            call.respond(HttpStatusCode.OK, response)
        }

        post("/refresh") {
            val request = call.receive<RefreshRequest>()
            val response = AuthService.refresh(request.refreshToken)
            call.respond(HttpStatusCode.OK, response)
        }

        post("/logout") {
            val request = call.receive<RefreshRequest>()
            AuthService.logout(request.refreshToken)
            call.respond(HttpStatusCode.OK, mapOf("message" to "Logged out successfully"))
        }

        authenticate("auth-jwt") {
            get("/me") {
                val principal = call.principal<UserPrincipal>()!!
                val user = AuthService.getMe(principal.userId)
                call.respond(HttpStatusCode.OK, user)
            }

            patch("/me") {
                val principal = call.principal<UserPrincipal>()!!
                val request = call.receive<UpdateProfileRequest>()
                val user = AuthService.updateProfile(principal.userId, request)
                call.respond(HttpStatusCode.OK, user)
            }

            post("/change-password") {
                val principal = call.principal<UserPrincipal>()!!
                val request = call.receive<ChangePasswordRequest>()
                AuthService.changePassword(principal.userId, request)
                call.respond(HttpStatusCode.OK, mapOf("message" to "Password changed successfully"))
            }
        }
    }
}
