package com.fitness.service.auth

import at.favre.lib.crypto.bcrypt.BCrypt
import com.fitness.dto.AuthResponse
import com.fitness.dto.RegisterRequest
import com.fitness.dto.RegisterResponse
import com.fitness.dto.TrainerInfoDTO
import com.fitness.entity.RefreshToken
import com.fitness.entity.User
import com.fitness.entity.VerificationToken
import com.fitness.mapper.UserMapper
import com.fitness.repository.RefreshTokenRepository
import com.fitness.repository.UserRepository
import com.fitness.repository.VerificationTokenRepository
import com.fitness.security.JwtService
import com.fitness.service.EmailService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

@Service
class RegistrationService(
    private val userRepository: UserRepository,
    private val verificationTokenRepository: VerificationTokenRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val emailService: EmailService,
    private val jwtService: JwtService,
    private val userMapper: UserMapper
) {
    @Transactional
    fun register(request: RegisterRequest): RegisterResponse {
        if (!isValidEmail(request.email)) {
            throw IllegalArgumentException("Invalid email format")
        }

        if (!isValidPassword(request.password)) {
            throw IllegalArgumentException("Password must be at least 8 characters and contain uppercase, lowercase and number")
        }

        if (userRepository.existsByEmail(request.email.lowercase())) {
            throw IllegalArgumentException("Email already registered")
        }

        // Validate trainer code (required)
        val trainer = userRepository.findByInviteCode(request.trainerCode)
            ?: throw IllegalArgumentException("Invalid trainer code")

        if (trainer.role != "admin") {
            throw IllegalArgumentException("Invalid trainer code")
        }

        val passwordHash = BCrypt.withDefaults().hashToString(10, request.password.toCharArray())

        val user = userRepository.save(
            User(
                email = request.email.lowercase(),
                passwordHash = passwordHash,
                firstName = request.firstName,
                lastName = request.lastName,
                phone = request.phone,
                emailVerified = false,
                trainerId = trainer.id
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
            user = userMapper.toDTO(verifiedUser)
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

    fun getTrainerByCode(code: String): TrainerInfoDTO {
        val trainer = userRepository.findByInviteCode(code)
            ?: throw NoSuchElementException("Trainer not found")

        if (trainer.role != "admin") {
            throw NoSuchElementException("Trainer not found")
        }

        return userMapper.toTrainerInfoDTO(trainer)
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
        return hasUppercase && hasLowercase && hasDigit
    }
}
