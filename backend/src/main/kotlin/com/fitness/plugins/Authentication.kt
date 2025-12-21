package com.fitness.plugins

import com.fitness.config.JwtConfig
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*

fun Application.configureAuthentication() {
    install(Authentication) {
        jwt("auth-jwt") {
            realm = "fitness-app"
            verifier(
                com.auth0.jwt.JWT
                    .require(JwtConfig.algorithm)
                    .withAudience(JwtConfig.getAudience())
                    .withIssuer(JwtConfig.getIssuer())
                    .build()
            )
            validate { credential ->
                val userId = credential.payload.getClaim("userId").asString()
                val email = credential.payload.getClaim("email").asString()
                val role = credential.payload.getClaim("role").asString()
                val type = credential.payload.getClaim("type").asString()

                if (userId != null && email != null && type == "access") {
                    UserPrincipal(userId, email, role ?: "client")
                } else {
                    null
                }
            }
        }

        jwt("auth-admin") {
            realm = "fitness-app-admin"
            verifier(
                com.auth0.jwt.JWT
                    .require(JwtConfig.algorithm)
                    .withAudience(JwtConfig.getAudience())
                    .withIssuer(JwtConfig.getIssuer())
                    .build()
            )
            validate { credential ->
                val userId = credential.payload.getClaim("userId").asString()
                val email = credential.payload.getClaim("email").asString()
                val role = credential.payload.getClaim("role").asString()
                val type = credential.payload.getClaim("type").asString()

                if (userId != null && email != null && type == "access" && role == "admin") {
                    UserPrincipal(userId, email, role)
                } else {
                    null
                }
            }
        }
    }
}

data class UserPrincipal(
    val userId: String,
    val email: String,
    val role: String
) : Principal
