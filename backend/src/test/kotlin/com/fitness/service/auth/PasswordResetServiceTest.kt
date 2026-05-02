package com.fitness.service.auth

import at.favre.lib.crypto.bcrypt.BCrypt
import com.fitness.entity.PasswordResetToken
import com.fitness.entity.User
import com.fitness.repository.PasswordResetTokenRepository
import com.fitness.repository.UserRepository
import com.fitness.security.RateLimiter
import com.fitness.service.EmailService
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.Optional
import java.util.UUID

class PasswordResetServiceTest {

    private val userRepository = mockk<UserRepository>()
    private val passwordResetTokenRepository = mockk<PasswordResetTokenRepository>(relaxed = true)
    private val emailService = mockk<EmailService>(relaxed = true)
    private val rateLimiter = mockk<RateLimiter>(relaxed = true)
    private val service = PasswordResetService(
        userRepository,
        passwordResetTokenRepository,
        emailService,
        rateLimiter,
    )

    @Test
    fun `requestPasswordReset silently returns when rate limited`() {
        every { rateLimiter.isBlocked("reset:client@example.com") } returns true

        service.requestPasswordReset("Client@Example.com")

        verify(exactly = 0) { rateLimiter.recordAttempt(any()) }
        verify(exactly = 0) { userRepository.findByEmail(any()) }
        verify(exactly = 0) { passwordResetTokenRepository.save(any()) }
    }

    @Test
    fun `requestPasswordReset avoids email enumeration for unknown user`() {
        every { rateLimiter.isBlocked("reset:missing@example.com") } returns false
        every { userRepository.findByEmail("missing@example.com") } returns null

        service.requestPasswordReset("Missing@Example.com")

        verify(exactly = 1) { rateLimiter.recordAttempt("reset:missing@example.com") }
        verify(exactly = 0) { passwordResetTokenRepository.save(any()) }
        verify(exactly = 0) { emailService.sendPasswordResetEmail(any(), any(), any()) }
    }

    @Test
    fun `requestPasswordReset replaces old token and sends email`() {
        val userId = UUID.randomUUID()
        val user = user(userId, email = "client@example.com", firstName = "Eva")
        val tokenSlot = slot<PasswordResetToken>()
        every { rateLimiter.isBlocked("reset:client@example.com") } returns false
        every { userRepository.findByEmail("client@example.com") } returns user
        every { passwordResetTokenRepository.save(capture(tokenSlot)) } answers { firstArg() }

        service.requestPasswordReset("Client@Example.com")

        assertEquals(userId, tokenSlot.captured.userId)
        verify(exactly = 1) { passwordResetTokenRepository.deleteByUserId(userId) }
        verify(exactly = 1) {
            emailService.sendPasswordResetEmail("client@example.com", tokenSlot.captured.token, "Eva")
        }
    }

    @Test
    fun `resetPassword deletes expired token and rejects request`() {
        val token = "expired-token"
        every { passwordResetTokenRepository.findByToken(token) } returns PasswordResetToken(
            userId = UUID.randomUUID(),
            token = token,
            expiresAt = Instant.now().minusSeconds(60),
        )

        val ex = assertThrows(IllegalArgumentException::class.java) {
            service.resetPassword(token, "NewPass2")
        }

        assertEquals("Reset token has expired", ex.message)
        verify(exactly = 1) { passwordResetTokenRepository.deleteByToken(token) }
        verify(exactly = 0) { userRepository.save(any()) }
    }

    @Test
    fun `resetPassword validates new password policy before saving`() {
        val token = "valid-token"
        every { passwordResetTokenRepository.findByToken(token) } returns PasswordResetToken(
            userId = UUID.randomUUID(),
            token = token,
            expiresAt = Instant.now().plusSeconds(600),
        )

        val ex = assertThrows(IllegalArgumentException::class.java) {
            service.resetPassword(token, "weak")
        }

        assertEquals("Password must be at least 8 characters and contain uppercase, lowercase and number", ex.message)
        verify(exactly = 0) { userRepository.findById(any()) }
        verify(exactly = 0) { userRepository.save(any()) }
    }

    @Test
    fun `resetPassword stores new hash and consumes token`() {
        val token = "valid-token"
        val userId = UUID.randomUUID()
        val savedSlot = slot<User>()
        every { passwordResetTokenRepository.findByToken(token) } returns PasswordResetToken(
            userId = userId,
            token = token,
            expiresAt = Instant.now().plusSeconds(600),
        )
        every { userRepository.findById(userId) } returns Optional.of(
            user(userId, email = "client@example.com", passwordHash = passwordHash("OldPass1")),
        )
        every { userRepository.save(capture(savedSlot)) } answers { firstArg() }

        service.resetPassword(token, "NewPass2")

        assertTrue(BCrypt.verifyer().verify("NewPass2".toCharArray(), savedSlot.captured.passwordHash).verified)
        verify(exactly = 1) { passwordResetTokenRepository.deleteByToken(token) }
    }

    private fun user(
        id: UUID,
        email: String,
        firstName: String? = null,
        passwordHash: String = "hash",
    ) = User(
        id = id,
        email = email,
        passwordHash = passwordHash,
        firstName = firstName,
    )

    private fun passwordHash(password: String): String =
        BCrypt.withDefaults().hashToString(10, password.toCharArray())
}
