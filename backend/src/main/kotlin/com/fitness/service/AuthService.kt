package com.fitness.service

import at.favre.lib.crypto.bcrypt.BCrypt
import com.fitness.dto.*
import com.fitness.entity.RefreshToken
import com.fitness.entity.User
import com.fitness.repository.RefreshTokenRepository
import com.fitness.repository.UserRepository
import com.fitness.security.JwtService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.*

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val jwtService: JwtService
) {

    @Transactional
    fun register(request: RegisterRequest): AuthResponse {
        if (!isValidEmail(request.email)) {
            throw IllegalArgumentException("Invalid email format")
        }

        if (request.password.length < 8) {
            throw IllegalArgumentException("Password must be at least 8 characters")
        }

        if (userRepository.existsByEmail(request.email.lowercase())) {
            throw IllegalArgumentException("Email already registered")
        }

        val passwordHash = BCrypt.withDefaults().hashToString(10, request.password.toCharArray())

        val user = userRepository.save(
            User(
                email = request.email.lowercase(),
                passwordHash = passwordHash,
                firstName = request.firstName,
                lastName = request.lastName,
                phone = request.phone
            )
        )

        val accessToken = jwtService.generateAccessToken(user.id.toString(), user.email, user.role)
        val refreshToken = jwtService.generateRefreshToken(user.id.toString())

        refreshTokenRepository.save(
            RefreshToken(
                userId = user.id,
                token = refreshToken,
                expiresAt = jwtService.getRefreshExpirationDate().toInstant()
            )
        )

        return AuthResponse(
            accessToken = accessToken,
            refreshToken = refreshToken,
            user = user.toDTO()
        )
    }

    @Transactional
    fun login(request: LoginRequest): AuthResponse {
        val user = userRepository.findByEmail(request.email.lowercase())
            ?: throw IllegalArgumentException("Invalid email or password")

        val result = BCrypt.verifyer().verify(request.password.toCharArray(), user.passwordHash)
        if (!result.verified) {
            throw IllegalArgumentException("Invalid email or password")
        }

        val accessToken = jwtService.generateAccessToken(user.id.toString(), user.email, user.role)
        val refreshToken = jwtService.generateRefreshToken(user.id.toString())

        refreshTokenRepository.save(
            RefreshToken(
                userId = user.id,
                token = refreshToken,
                expiresAt = jwtService.getRefreshExpirationDate().toInstant()
            )
        )

        return AuthResponse(
            accessToken = accessToken,
            refreshToken = refreshToken,
            user = user.toDTO()
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

        refreshTokenRepository.deleteByToken(refreshToken)

        val newAccessToken = jwtService.generateAccessToken(user.id.toString(), user.email, user.role)
        val newRefreshToken = jwtService.generateRefreshToken(user.id.toString())

        refreshTokenRepository.save(
            RefreshToken(
                userId = user.id,
                token = newRefreshToken,
                expiresAt = jwtService.getRefreshExpirationDate().toInstant()
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

    fun getMe(userId: String): UserDTO {
        val user = userRepository.findById(UUID.fromString(userId))
            .orElseThrow { NoSuchElementException("User not found") }
        return user.toDTO()
    }

    @Transactional
    fun updateProfile(userId: String, request: UpdateProfileRequest): UserDTO {
        val user = userRepository.findById(UUID.fromString(userId))
            .orElseThrow { NoSuchElementException("User not found") }

        val updated = user.copy(
            firstName = request.firstName ?: user.firstName,
            lastName = request.lastName ?: user.lastName,
            phone = request.phone ?: user.phone,
            locale = request.locale ?: user.locale,
            theme = request.theme ?: user.theme,
            updatedAt = Instant.now()
        )

        return userRepository.save(updated).toDTO()
    }

    @Transactional
    fun changePassword(userId: String, request: ChangePasswordRequest) {
        val user = userRepository.findById(UUID.fromString(userId))
            .orElseThrow { NoSuchElementException("User not found") }

        val result = BCrypt.verifyer().verify(request.currentPassword.toCharArray(), user.passwordHash)
        if (!result.verified) {
            throw IllegalArgumentException("Current password is incorrect")
        }

        if (request.newPassword.length < 8) {
            throw IllegalArgumentException("New password must be at least 8 characters")
        }

        val newHash = BCrypt.withDefaults().hashToString(10, request.newPassword.toCharArray())
        val updated = user.copy(passwordHash = newHash, updatedAt = Instant.now())
        userRepository.save(updated)
    }

    private fun isValidEmail(email: String): Boolean {
        return email.contains("@") && email.substringAfter("@").contains(".")
    }

    private fun User.toDTO() = UserDTO(
        id = id.toString(),
        email = email,
        firstName = firstName,
        lastName = lastName,
        phone = phone,
        role = role,
        credits = credits,
        locale = locale,
        theme = theme,
        createdAt = createdAt.toString()
    )
}
