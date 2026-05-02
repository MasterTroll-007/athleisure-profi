package com.fitness.service.auth

import at.favre.lib.crypto.bcrypt.BCrypt
import com.fitness.dto.ChangePasswordRequest
import com.fitness.dto.UpdateProfileRequest
import com.fitness.dto.UserDTO
import com.fitness.entity.User
import com.fitness.mapper.UserMapper
import com.fitness.repository.UserRepository
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

class ProfileServiceTest {

    private val userRepository = mockk<UserRepository>()
    private val userMapper = mockk<UserMapper>()
    private val service = ProfileService(userRepository, userMapper)

    @Test
    fun `getMe loads user and maps DTO`() {
        val userId = UUID.randomUUID()
        val user = user(id = userId, email = "client@example.com")
        val dto = userDto(user)
        every { userRepository.findById(userId) } returns Optional.of(user)
        every { userMapper.toDTO(user) } returns dto

        val result = service.getMe(userId.toString())

        assertEquals(dto, result)
    }

    @Test
    fun `updateProfile updates provided fields and preserves missing fields`() {
        val userId = UUID.randomUUID()
        val existing = user(
            id = userId,
            email = "client@example.com",
            firstName = "Old",
            lastName = "Name",
            phone = "111",
            locale = "cs",
            theme = "dark",
            emailRemindersEnabled = true,
            reminderHoursBefore = 24,
        )
        val savedSlot = slot<User>()
        every { userRepository.findById(userId) } returns Optional.of(existing)
        every { userRepository.save(capture(savedSlot)) } answers { firstArg() }
        every { userMapper.toDTO(any()) } answers { userDto(firstArg()) }

        val result = service.updateProfile(
            userId.toString(),
            UpdateProfileRequest(
                firstName = "New",
                lastName = null,
                phone = "222",
                locale = "en",
                theme = null,
                emailRemindersEnabled = false,
                reminderHoursBefore = 6,
            ),
        )

        assertEquals("New", savedSlot.captured.firstName)
        assertEquals("Name", savedSlot.captured.lastName)
        assertEquals("222", savedSlot.captured.phone)
        assertEquals("en", savedSlot.captured.locale)
        assertEquals("dark", savedSlot.captured.theme)
        assertEquals(false, savedSlot.captured.emailRemindersEnabled)
        assertEquals(6, savedSlot.captured.reminderHoursBefore)
        assertEquals("New", result.firstName)
    }

    @Test
    fun `changePassword rejects wrong current password`() {
        val userId = UUID.randomUUID()
        every { userRepository.findById(userId) } returns Optional.of(
            user(id = userId, email = "client@example.com", passwordHash = passwordHash("OldPass1")),
        )

        val ex = assertThrows(IllegalArgumentException::class.java) {
            service.changePassword(
                userId.toString(),
                ChangePasswordRequest(currentPassword = "WrongPass1", newPassword = "NewPass2"),
            )
        }

        assertEquals("Current password is incorrect", ex.message)
        verify(exactly = 0) { userRepository.save(any()) }
    }

    @Test
    fun `changePassword validates new password policy`() {
        val userId = UUID.randomUUID()
        every { userRepository.findById(userId) } returns Optional.of(
            user(id = userId, email = "client@example.com", passwordHash = passwordHash("OldPass1")),
        )

        val ex = assertThrows(IllegalArgumentException::class.java) {
            service.changePassword(
                userId.toString(),
                ChangePasswordRequest(currentPassword = "OldPass1", newPassword = "weak"),
            )
        }

        assertEquals("Password must be at least 8 characters and contain uppercase, lowercase and number", ex.message)
        verify(exactly = 0) { userRepository.save(any()) }
    }

    @Test
    fun `changePassword stores a new bcrypt hash`() {
        val userId = UUID.randomUUID()
        val savedSlot = slot<User>()
        every { userRepository.findById(userId) } returns Optional.of(
            user(id = userId, email = "client@example.com", passwordHash = passwordHash("OldPass1")),
        )
        every { userRepository.save(capture(savedSlot)) } answers { firstArg() }

        service.changePassword(
            userId.toString(),
            ChangePasswordRequest(currentPassword = "OldPass1", newPassword = "NewPass2"),
        )

        assertTrue(BCrypt.verifyer().verify("NewPass2".toCharArray(), savedSlot.captured.passwordHash).verified)
    }

    private fun user(
        id: UUID,
        email: String,
        passwordHash: String = "hash",
        firstName: String? = null,
        lastName: String? = null,
        phone: String? = null,
        locale: String = "cs",
        theme: String = "dark",
        emailRemindersEnabled: Boolean = true,
        reminderHoursBefore: Int = 24,
    ) = User(
        id = id,
        email = email,
        passwordHash = passwordHash,
        firstName = firstName,
        lastName = lastName,
        phone = phone,
        locale = locale,
        theme = theme,
        emailRemindersEnabled = emailRemindersEnabled,
        reminderHoursBefore = reminderHoursBefore,
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

    private fun passwordHash(password: String): String =
        BCrypt.withDefaults().hashToString(10, password.toCharArray())
}
