package com.fitness.service.auth

import at.favre.lib.crypto.bcrypt.BCrypt
import com.fitness.dto.AuthResponse
import com.fitness.dto.LoginRequest
import com.fitness.dto.TokenResponse
import com.fitness.entity.RefreshToken
import com.fitness.mapper.UserMapper
import com.fitness.repository.RefreshTokenRepository
import com.fitness.repository.UserRepository
import com.fitness.security.JwtService
import com.fitness.security.RateLimiter
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class AuthenticationService(
    private val userRepository: UserRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val jwtService: JwtService,
    private val rateLimiter: RateLimiter,
    private val userMapper: UserMapper
) {
    @Transactional
    fun login(request: LoginRequest): AuthResponse {
        val rateLimitKey = request.email.lowercase()

        // Check if blocked due to too many attempts
        if (rateLimiter.isBlocked(rateLimitKey)) {
            val remaining = rateLimiter.getRemainingBlockTime(rateLimitKey)
            throw IllegalArgumentException("Too many login attempts. Try again in $remaining seconds.")
        }

        val user = userRepository.findByEmail(request.email.lowercase())
        if (user == null) {
            rateLimiter.recordAttempt(rateLimitKey)
            throw IllegalArgumentException("Invalid email or password")
        }

        val result = BCrypt.verifyer().verify(request.password.toCharArray(), user.passwordHash)
        if (!result.verified) {
            rateLimiter.recordAttempt(rateLimitKey)
            throw IllegalArgumentException("Invalid email or password")
        }

        if (!user.emailVerified) {
            throw IllegalArgumentException("Please verify your email before logging in")
        }

        // Clear rate limit on successful login
        rateLimiter.clearAttempts(rateLimitKey)

        val accessToken = jwtService.generateAccessToken(user.id.toString(), user.email, user.role)
        val refreshToken = jwtService.generateRefreshToken(user.id.toString(), request.rememberMe)

        refreshTokenRepository.save(
            RefreshToken(
                userId = user.id,
                token = refreshToken,
                expiresAt = jwtService.getRefreshExpirationDate(request.rememberMe).toInstant()
            )
        )

        return AuthResponse(
            accessToken = accessToken,
            refreshToken = refreshToken,
            user = userMapper.toDTO(user)
        )
    }

    @Transactional
    fun refresh(refreshToken: String): TokenResponse {
        val claims = jwtService.validateToken(refreshToken)
            ?: throw IllegalArgumentException("Invalid refresh token")

        if (claims["type"] != "refresh") {
            throw IllegalArgumentException("Invalid token type")
        }

        val storedToken = refreshTokenRepository.findByToken(refreshToken)
            ?: throw IllegalArgumentException("Refresh token not found")

        if (storedToken.expiresAt.isBefore(Instant.now())) {
            refreshTokenRepository.deleteByToken(refreshToken)
            throw IllegalArgumentException("Refresh token expired")
        }

        val user = userRepository.findById(storedToken.userId)
            .orElseThrow { IllegalArgumentException("User not found") }

        // Preserve rememberMe flag from original token
        val rememberMe = claims["rememberMe"] as? Boolean ?: false

        refreshTokenRepository.deleteByToken(refreshToken)

        val newAccessToken = jwtService.generateAccessToken(user.id.toString(), user.email, user.role)
        val newRefreshToken = jwtService.generateRefreshToken(user.id.toString(), rememberMe)

        refreshTokenRepository.save(
            RefreshToken(
                userId = user.id,
                token = newRefreshToken,
                expiresAt = jwtService.getRefreshExpirationDate(rememberMe).toInstant()
            )
        )

        return TokenResponse(
            accessToken = newAccessToken,
            refreshToken = newRefreshToken
        )
    }

    @Transactional
    fun logout(refreshToken: String) {
        refreshTokenRepository.deleteByToken(refreshToken)
    }
}
