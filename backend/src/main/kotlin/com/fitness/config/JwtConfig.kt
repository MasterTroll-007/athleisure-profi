package com.fitness.config

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import java.util.*

object JwtConfig {
    private val secret: String = System.getenv("JWT_SECRET") ?: "your-super-secret-jwt-key-change-in-production"
    private val issuer = "fitness-app"
    private val audience = "fitness-users"

    private val accessExpiration: Long = System.getenv("JWT_ACCESS_EXPIRATION")?.toLongOrNull() ?: 900000L // 15 minutes
    private val refreshExpiration: Long = System.getenv("JWT_REFRESH_EXPIRATION")?.toLongOrNull() ?: 604800000L // 7 days

    val algorithm: Algorithm = Algorithm.HMAC256(secret)

    fun generateAccessToken(userId: String, email: String, role: String): String {
        return JWT.create()
            .withAudience(audience)
            .withIssuer(issuer)
            .withClaim("userId", userId)
            .withClaim("email", email)
            .withClaim("role", role)
            .withClaim("type", "access")
            .withExpiresAt(Date(System.currentTimeMillis() + accessExpiration))
            .sign(algorithm)
    }

    fun generateRefreshToken(userId: String): String {
        return JWT.create()
            .withAudience(audience)
            .withIssuer(issuer)
            .withClaim("userId", userId)
            .withClaim("type", "refresh")
            .withExpiresAt(Date(System.currentTimeMillis() + refreshExpiration))
            .sign(algorithm)
    }

    fun getRefreshExpirationDate(): Date {
        return Date(System.currentTimeMillis() + refreshExpiration)
    }

    fun verifyToken(token: String): com.auth0.jwt.interfaces.DecodedJWT? {
        return try {
            JWT.require(algorithm)
                .withAudience(audience)
                .withIssuer(issuer)
                .build()
                .verify(token)
        } catch (e: Exception) {
            null
        }
    }

    fun getAudience() = audience
    fun getIssuer() = issuer
}
