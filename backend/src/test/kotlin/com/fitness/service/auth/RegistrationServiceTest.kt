package com.fitness.service.auth

import com.fitness.dto.RegisterRequest
import com.fitness.dto.TrainerInfoDTO
import com.fitness.dto.UserDTO
import com.fitness.entity.RefreshToken
import com.fitness.entity.User
import com.fitness.entity.VerificationToken
import com.fitness.mapper.UserMapper
import com.fitness.repository.RefreshTokenRepository
import com.fitness.repository.UserRepository
import com.fitness.repository.VerificationTokenRepository
import com.fitness.security.JwtService
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
import java.util.Date
import java.util.Optional
import java.util.UUID

class RegistrationServiceTest {

    private val userRepository = mockk<UserRepository>()
    private val verificationTokenRepository = mockk<VerificationTokenRepository>(relaxed = true)
    private val refreshTokenRepository = mockk<RefreshTokenRepository>(relaxed = true)
    private val emailService = mockk<EmailService>(relaxed = true)
    private val jwtService = mockk<JwtService>()
    private val userMapper = mockk<UserMapper>()
    private val rateLimiter = mockk<RateLimiter>(relaxed = true)
    private val service = RegistrationService(
        userRepository,
        verificationTokenRepository,
        refreshTokenRepository,
        emailService,
        jwtService,
        userMapper,
        rateLimiter,
    )

    @Test
    fun `register creates unverified client for trainer invite and sends verification email`() {
        val trainerId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val savedUserSlot = slot<User>()
        val tokenSlot = slot<VerificationToken>()
        every { rateLimiter.isBlocked("register:client@example.com") } returns false
        every { userRepository.existsByEmail("client@example.com") } returns false
        every { userRepository.findByInviteCode("TRAINER") } returns user(
            id = trainerId,
            email = "trainer@example.com",
            role = "admin",
        )
        every { userRepository.save(capture(savedUserSlot)) } answers { firstArg<User>().copy(id = userId) }
        every { verificationTokenRepository.save(capture(tokenSlot)) } answers { firstArg() }

        val response = service.register(
            RegisterRequest(
                email = "Client@Example.com",
                password = "Password1",
                firstName = "Eva",
                lastName = "Novak",
                phone = "123",
                trainerCode = "TRAINER",
            ),
        )

        assertEquals("client@example.com", response.email)
        assertEquals("client@example.com", savedUserSlot.captured.email)
        assertEquals("Eva", savedUserSlot.captured.firstName)
        assertEquals("Novak", savedUserSlot.captured.lastName)
        assertEquals("123", savedUserSlot.captured.phone)
        assertEquals(false, savedUserSlot.captured.emailVerified)
        assertEquals(trainerId, savedUserSlot.captured.trainerId)
        assertEquals(userId, tokenSlot.captured.userId)
        verify(exactly = 1) { rateLimiter.recordAttempt("register:client@example.com") }
        verify(exactly = 1) {
            emailService.sendVerificationEmail("client@example.com", tokenSlot.captured.token, "Eva")
        }
    }

    @Test
    fun `register rejects invalid email before persistence`() {
        every { rateLimiter.isBlocked("register:bad-email") } returns false

        val ex = assertThrows(IllegalArgumentException::class.java) {
            service.register(registerRequest(email = "bad-email"))
        }

        assertEquals("Invalid email format", ex.message)
        verify(exactly = 0) { userRepository.save(any()) }
        verify(exactly = 0) { emailService.sendVerificationEmail(any(), any(), any()) }
    }

    @Test
    fun `register rejects non admin trainer invite`() {
        every { rateLimiter.isBlocked("register:client@example.com") } returns false
        every { userRepository.existsByEmail("client@example.com") } returns false
        every { userRepository.findByInviteCode("TRAINER") } returns user(
            id = UUID.randomUUID(),
            email = "trainer@example.com",
            role = "client",
        )

        val ex = assertThrows(IllegalArgumentException::class.java) {
            service.register(registerRequest())
        }

        assertEquals("Invalid trainer code", ex.message)
        verify(exactly = 0) { userRepository.save(any()) }
        verify(exactly = 0) { verificationTokenRepository.save(any()) }
    }

    @Test
    fun `verifyEmail marks user verified creates refresh token and returns auth response`() {
        val userId = UUID.randomUUID()
        val token = "verify-token"
        val savedUserSlot = slot<User>()
        val refreshTokenSlot = slot<RefreshToken>()
        val user = user(id = userId, email = "client@example.com", role = "client", emailVerified = false)
        every { verificationTokenRepository.findByToken(token) } returns VerificationToken(
            userId = userId,
            token = token,
            expiresAt = Instant.now().plusSeconds(600),
        )
        every { userRepository.findById(userId) } returns Optional.of(user)
        every { userRepository.save(capture(savedUserSlot)) } answers { firstArg() }
        every { jwtService.generateAccessToken(userId.toString(), "client@example.com", "client") } returns "access-token"
        every { jwtService.generateRefreshToken(userId.toString()) } returns "refresh-token"
        every { jwtService.getRefreshExpirationDate() } returns Date.from(Instant.now().plusSeconds(3600))
        every { refreshTokenRepository.save(capture(refreshTokenSlot)) } answers { firstArg() }
        every { userMapper.toDTO(any()) } answers { userDto(firstArg()) }

        val response = service.verifyEmail(token)

        assertTrue(savedUserSlot.captured.emailVerified)
        assertEquals(userId, refreshTokenSlot.captured.userId)
        assertEquals("refresh-token", refreshTokenSlot.captured.token)
        assertEquals("access-token", response.accessToken)
        assertEquals("refresh-token", response.refreshToken)
        assertEquals("client@example.com", response.user.email)
        verify(exactly = 1) { verificationTokenRepository.deleteByToken(token) }
    }

    @Test
    fun `verifyEmail deletes expired token and rejects request`() {
        val token = "expired-token"
        every { verificationTokenRepository.findByToken(token) } returns VerificationToken(
            userId = UUID.randomUUID(),
            token = token,
            expiresAt = Instant.now().minusSeconds(60),
        )

        val ex = assertThrows(IllegalArgumentException::class.java) {
            service.verifyEmail(token)
        }

        assertEquals("Verification token has expired", ex.message)
        verify(exactly = 1) { verificationTokenRepository.deleteByToken(token) }
        verify(exactly = 0) { userRepository.save(any()) }
    }

    @Test
    fun `getTrainerByCode maps only admin users`() {
        val admin = user(
            id = UUID.randomUUID(),
            email = "trainer@example.com",
            role = "admin",
            firstName = "Jana",
            lastName = "Pankova",
        )
        every { userRepository.findByInviteCode("TRAINER") } returns admin
        every { userMapper.toTrainerInfoDTO(admin) } returns TrainerInfoDTO("Jana", "Pankova")

        val dto = service.getTrainerByCode("TRAINER")

        assertEquals("Jana", dto.firstName)
        assertEquals("Pankova", dto.lastName)
    }

    @Test
    fun `getTrainerByCode hides non admin users`() {
        every { userRepository.findByInviteCode("CLIENT") } returns user(
            id = UUID.randomUUID(),
            email = "client@example.com",
            role = "client",
        )

        assertThrows(NoSuchElementException::class.java) {
            service.getTrainerByCode("CLIENT")
        }
    }

    private fun registerRequest(
        email: String = "client@example.com",
        password: String = "Password1",
    ) = RegisterRequest(
        email = email,
        password = password,
        firstName = "Eva",
        lastName = "Novak",
        phone = "123",
        trainerCode = "TRAINER",
    )

    private fun user(
        id: UUID,
        email: String,
        role: String,
        firstName: String? = null,
        lastName: String? = null,
        emailVerified: Boolean = true,
    ) = User(
        id = id,
        email = email,
        passwordHash = "hash",
        firstName = firstName,
        lastName = lastName,
        role = role,
        emailVerified = emailVerified,
    )

    private fun userDto(user: User) = UserDTO(
        id = user.id.toString(),
        email = user.email,
        firstName = user.firstName,
        lastName = user.lastName,
        phone = user.phone,
        role = user.role,
        credits = user.credits,
        locale = user.locale,
        theme = user.theme,
        trainerId = user.trainerId?.toString(),
        trainerName = null,
        calendarStartHour = user.calendarStartHour,
        calendarEndHour = user.calendarEndHour,
        isBlocked = user.isBlocked,
        emailRemindersEnabled = user.emailRemindersEnabled,
        reminderHoursBefore = user.reminderHoursBefore,
        createdAt = Instant.parse("2026-04-18T08:15:30Z").toString(),
    )
}
