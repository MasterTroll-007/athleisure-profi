package com.fitness.service.auth

import at.favre.lib.crypto.bcrypt.BCrypt
import com.fitness.entity.PasswordResetToken
import com.fitness.repository.PasswordResetTokenRepository
import com.fitness.repository.UserRepository
import com.fitness.service.EmailService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

@Service
class PasswordResetService(
    private val userRepository: UserRepository,
    private val passwordResetTokenRepository: PasswordResetTokenRepository,
    private val emailService: EmailService
) {
    private val logger = LoggerFactory.getLogger(PasswordResetService::class.java)

    @Transactional
    fun requestPasswordReset(email: String) {
        val user = userRepository.findByEmail(email.lowercase())

        // Always return success to prevent email enumeration
        if (user == null) {
            logger.info("Password reset requested for non-existent email: $email")
            return
        }

        // Delete any existing tokens for this user
        passwordResetTokenRepository.deleteByUserId(user.id)

        // Create new reset token (valid for 30 minutes - security best practice)
        // Shorter validity reduces window of opportunity for token interception
        val token = UUID.randomUUID().toString()
        passwordResetTokenRepository.save(
            PasswordResetToken(
                userId = user.id,
                token = token,
                expiresAt = Instant.now().plus(30, ChronoUnit.MINUTES)
            )
        )

        // Send password reset email
        emailService.sendPasswordResetEmail(user.email, token, user.firstName)
        logger.info("Password reset email sent to: ${user.email}")
    }

    @Transactional
    fun resetPassword(token: String, newPassword: String) {
        val resetToken = passwordResetTokenRepository.findByToken(token)
            ?: throw IllegalArgumentException("Invalid or expired reset token")

        if (resetToken.expiresAt.isBefore(Instant.now())) {
            passwordResetTokenRepository.deleteByToken(token)
            throw IllegalArgumentException("Reset token has expired")
        }

        if (!isValidPassword(newPassword)) {
            throw IllegalArgumentException("Password must be at least 8 characters and contain uppercase, lowercase and number")
        }

        val user = userRepository.findById(resetToken.userId)
            .orElseThrow { IllegalArgumentException("User not found") }

        // Update password
        val passwordHash = BCrypt.withDefaults().hashToString(10, newPassword.toCharArray())
        userRepository.save(user.copy(passwordHash = passwordHash, updatedAt = Instant.now()))

        // Delete the reset token
        passwordResetTokenRepository.deleteByToken(token)

        logger.info("Password reset successfully for user: ${user.email}")
    }

    private fun isValidPassword(password: String): Boolean {
        if (password.length < 8) return false
        val hasUppercase = password.any { it.isUpperCase() }
        val hasLowercase = password.any { it.isLowerCase() }
        val hasDigit = password.any { it.isDigit() }
        return hasUppercase && hasLowercase && hasDigit
    }
}
