package com.fitness.service

import at.favre.lib.crypto.bcrypt.BCrypt
import com.fitness.dto.*
import com.fitness.entity.RefreshToken
import com.fitness.entity.User
import com.fitness.entity.VerificationToken
import com.fitness.repository.RefreshTokenRepository
import com.fitness.repository.UserRepository
import com.fitness.repository.VerificationTokenRepository
import com.fitness.security.JwtService
import com.fitness.security.RateLimiter
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val verificationTokenRepository: VerificationTokenRepository,
    private val jwtService: JwtService,
    private val emailService: EmailService,
    private val rateLimiter: RateLimiter
) {

    @Transactional
    fun register(request: RegisterRequest): RegisterResponse {
        if (!isValidEmail(request.email)) {
            throw IllegalArgumentException("Invalid email format")
        }

        if (!isValidPassword(request.password)) {
            throw IllegalArgumentException("Password must be at least 8 characters and contain uppercase, lowercase, number, and special character")
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
                phone = request.phone,
                emailVerified = false
            )
        )

        // Create verification token
        val token = UUID.randomUUID().toString()
        verificationTokenRepository.save(
            VerificationToken(
                userId = user.id,
                token = token,
                expiresAt = Instant.now().plus(24, ChronoUnit.HOURS)
            )
        )

        // Send verification email
        emailService.sendVerificationEmail(user.email, token, user.firstName)

        return RegisterResponse(
            message = "Registration successful. Please check your email to verify your account.",
            email = user.email
        )
    }

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

    @Transactional
    fun verifyEmail(token: String): AuthResponse {
        val verificationToken = verificationTokenRepository.findByToken(token)
            ?: throw IllegalArgumentException("Invalid verification token")

        if (verificationToken.expiresAt.isBefore(Instant.now())) {
            verificationTokenRepository.deleteByToken(token)
            throw IllegalArgumentException("Verification token has expired")
        }

        val user = userRepository.findById(verificationToken.userId)
            .orElseThrow { IllegalArgumentException("User not found") }

        // Mark email as verified
        val verifiedUser = userRepository.save(user.copy(emailVerified = true, updatedAt = Instant.now()))

        // Delete the verification token
        verificationTokenRepository.deleteByToken(token)

        // Generate tokens and log user in
        val accessToken = jwtService.generateAccessToken(verifiedUser.id.toString(), verifiedUser.email, verifiedUser.role)
        val refreshToken = jwtService.generateRefreshToken(verifiedUser.id.toString())

        refreshTokenRepository.save(
            RefreshToken(
                userId = verifiedUser.id,
                token = refreshToken,
                expiresAt = jwtService.getRefreshExpirationDate().toInstant()
            )
        )

        return AuthResponse(
            accessToken = accessToken,
            refreshToken = refreshToken,
            user = verifiedUser.toDTO()
        )
    }

    @Transactional
    fun resendVerificationEmail(email: String) {
        val user = userRepository.findByEmail(email.lowercase())
            ?: throw IllegalArgumentException("Email not found")

        if (user.emailVerified) {
            throw IllegalArgumentException("Email is already verified")
        }

        // Delete any existing tokens
        verificationTokenRepository.deleteByUserId(user.id)

        // Create new verification token
        val token = UUID.randomUUID().toString()
        verificationTokenRepository.save(
            VerificationToken(
                userId = user.id,
                token = token,
                expiresAt = Instant.now().plus(24, ChronoUnit.HOURS)
            )
        )

        // Send verification email
        emailService.sendVerificationEmail(user.email, token, user.firstName)
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

        if (!isValidPassword(request.newPassword)) {
            throw IllegalArgumentException("Password must be at least 8 characters and contain uppercase, lowercase, number, and special character")
        }

        val newHash = BCrypt.withDefaults().hashToString(10, request.newPassword.toCharArray())
        val updated = user.copy(passwordHash = newHash, updatedAt = Instant.now())
        userRepository.save(updated)
    }

    private fun isValidEmail(email: String): Boolean {
        val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
        return email.matches(emailRegex)
    }

    private fun isValidPassword(password: String): Boolean {
        if (password.length < 8) return false
        val hasUppercase = password.any { it.isUpperCase() }
        val hasLowercase = password.any { it.isLowerCase() }
        val hasDigit = password.any { it.isDigit() }
        val hasSpecial = password.any { it in "@\$!%*?&" }
        return hasUppercase && hasLowercase && hasDigit && hasSpecial
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
