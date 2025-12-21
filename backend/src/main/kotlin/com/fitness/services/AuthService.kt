package com.fitness.services

import at.favre.lib.crypto.bcrypt.BCrypt
import com.fitness.config.JwtConfig
import com.fitness.models.*
import com.fitness.repositories.RefreshTokenRepository
import com.fitness.repositories.UserRepository
import java.util.*

object AuthService {

    fun register(request: RegisterRequest): AuthResponse {
        // Validate email
        if (!isValidEmail(request.email)) {
            throw IllegalArgumentException("Invalid email format")
        }

        // Validate password
        if (request.password.length < 8) {
            throw IllegalArgumentException("Password must be at least 8 characters")
        }

        // Check if email exists
        if (UserRepository.emailExists(request.email)) {
            throw IllegalArgumentException("Email already registered")
        }

        // Hash password
        val passwordHash = BCrypt.withDefaults().hashToString(10, request.password.toCharArray())

        // Create user
        val user = UserRepository.create(
            email = request.email.lowercase(),
            passwordHash = passwordHash,
            firstName = request.firstName,
            lastName = request.lastName,
            phone = request.phone
        )

        // Generate tokens
        val accessToken = JwtConfig.generateAccessToken(user.id, user.email, user.role)
        val refreshToken = JwtConfig.generateRefreshToken(user.id)

        // Save refresh token
        RefreshTokenRepository.create(
            userId = UUID.fromString(user.id),
            token = refreshToken,
            expiresAt = JwtConfig.getRefreshExpirationDate().toInstant()
        )

        return AuthResponse(
            accessToken = accessToken,
            refreshToken = refreshToken,
            user = user
        )
    }

    fun login(request: LoginRequest): AuthResponse {
        // Find user
        val user = UserRepository.findByEmail(request.email.lowercase())
            ?: throw IllegalArgumentException("Invalid email or password")

        // Verify password
        val passwordHash = UserRepository.getPasswordHash(request.email.lowercase())
            ?: throw IllegalArgumentException("Invalid email or password")

        val result = BCrypt.verifyer().verify(request.password.toCharArray(), passwordHash)
        if (!result.verified) {
            throw IllegalArgumentException("Invalid email or password")
        }

        // Generate tokens
        val accessToken = JwtConfig.generateAccessToken(user.id, user.email, user.role)
        val refreshToken = JwtConfig.generateRefreshToken(user.id)

        // Save refresh token
        RefreshTokenRepository.create(
            userId = UUID.fromString(user.id),
            token = refreshToken,
            expiresAt = JwtConfig.getRefreshExpirationDate().toInstant()
        )

        return AuthResponse(
            accessToken = accessToken,
            refreshToken = refreshToken,
            user = user
        )
    }

    fun refresh(refreshToken: String): TokenResponse {
        // Verify token format
        val decoded = JwtConfig.verifyToken(refreshToken)
            ?: throw IllegalArgumentException("Invalid refresh token")

        if (decoded.getClaim("type").asString() != "refresh") {
            throw IllegalArgumentException("Invalid token type")
        }

        // Check if token exists in database
        val storedToken = RefreshTokenRepository.findByToken(refreshToken)
            ?: throw IllegalArgumentException("Refresh token not found")

        // Check if expired
        if (storedToken.expiresAt.isBefore(java.time.Instant.now())) {
            RefreshTokenRepository.delete(refreshToken)
            throw IllegalArgumentException("Refresh token expired")
        }

        // Get user
        val user = UserRepository.findById(storedToken.userId)
            ?: throw IllegalArgumentException("User not found")

        // Delete old refresh token
        RefreshTokenRepository.delete(refreshToken)

        // Generate new tokens
        val newAccessToken = JwtConfig.generateAccessToken(user.id, user.email, user.role)
        val newRefreshToken = JwtConfig.generateRefreshToken(user.id)

        // Save new refresh token
        RefreshTokenRepository.create(
            userId = UUID.fromString(user.id),
            token = newRefreshToken,
            expiresAt = JwtConfig.getRefreshExpirationDate().toInstant()
        )

        return TokenResponse(
            accessToken = newAccessToken,
            refreshToken = newRefreshToken
        )
    }

    fun logout(refreshToken: String) {
        RefreshTokenRepository.delete(refreshToken)
    }

    fun getMe(userId: String): UserDTO {
        return UserRepository.findById(UUID.fromString(userId))
            ?: throw NoSuchElementException("User not found")
    }

    fun updateProfile(userId: String, request: UpdateProfileRequest): UserDTO {
        return UserRepository.update(UUID.fromString(userId), request)
            ?: throw NoSuchElementException("User not found")
    }

    fun changePassword(userId: String, request: ChangePasswordRequest): Boolean {
        val user = UserRepository.findById(UUID.fromString(userId))
            ?: throw NoSuchElementException("User not found")

        // Verify current password
        val currentHash = UserRepository.getPasswordHash(user.email)
            ?: throw IllegalStateException("Password hash not found")

        val result = BCrypt.verifyer().verify(request.currentPassword.toCharArray(), currentHash)
        if (!result.verified) {
            throw IllegalArgumentException("Current password is incorrect")
        }

        // Validate new password
        if (request.newPassword.length < 8) {
            throw IllegalArgumentException("New password must be at least 8 characters")
        }

        // Hash and update
        val newHash = BCrypt.withDefaults().hashToString(10, request.newPassword.toCharArray())
        return UserRepository.updatePassword(UUID.fromString(userId), newHash)
    }

    private fun isValidEmail(email: String): Boolean {
        val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
        return email.matches(emailRegex)
    }
}
