package com.fitness.security

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.*
import javax.crypto.SecretKey

@Service
class JwtService(
    @Value("\${jwt.secret}") private val secret: String,
    @Value("\${jwt.access-expiration}") private val accessExpiration: Long,
    @Value("\${jwt.refresh-expiration}") private val refreshExpiration: Long,
    @Value("\${jwt.refresh-expiration-remember-me}") private val refreshExpirationRememberMe: Long
) {
    private val key: SecretKey = Keys.hmacShaKeyFor(secret.toByteArray())

    fun generateAccessToken(userId: String, email: String, role: String): String {
        return Jwts.builder()
            .subject(userId)
            .claim("email", email)
            .claim("role", role)
            .claim("type", "access")
            .issuedAt(Date())
            .expiration(Date(System.currentTimeMillis() + accessExpiration))
            .signWith(key)
            .compact()
    }

    fun generateRefreshToken(userId: String, rememberMe: Boolean = false): String {
        val expiration = if (rememberMe) refreshExpirationRememberMe else refreshExpiration
        return Jwts.builder()
            .subject(userId)
            .claim("type", "refresh")
            .claim("rememberMe", rememberMe)
            .issuedAt(Date())
            .expiration(Date(System.currentTimeMillis() + expiration))
            .signWith(key)
            .compact()
    }

    fun validateToken(token: String): Claims? {
        return try {
            Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .payload
        } catch (e: Exception) {
            null
        }
    }

    fun getUserIdFromToken(token: String): String? {
        return validateToken(token)?.subject
    }

    fun getRefreshExpirationDate(rememberMe: Boolean = false): Date {
        val expiration = if (rememberMe) refreshExpirationRememberMe else refreshExpiration
        return Date(System.currentTimeMillis() + expiration)
    }
}
