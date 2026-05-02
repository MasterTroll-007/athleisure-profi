package com.fitness.controller.admin

import com.fitness.dto.AdminSettingsDTO
import com.fitness.dto.UpdateAdminSettingsRequest
import com.fitness.entity.User
import com.fitness.repository.UserRepository
import com.fitness.security.UserPrincipal
import com.fitness.service.AdminSettingsService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.Optional
import java.util.UUID

class AdminSettingsControllerTest {
    private val userRepository = mockk<UserRepository>()
    private val adminSettingsService = mockk<AdminSettingsService>()
    private val controller = AdminSettingsController(userRepository, adminSettingsService)

    @Test
    fun `settings response includes invite link and adjacent booking flag`() {
        val adminId = UUID.randomUUID()
        every { userRepository.findById(adminId) } returns Optional.of(admin(adminId, inviteCode = "invite123"))

        val dto = controller.getSettings(principal(adminId)).body!!

        assertThat(dto.calendarStartHour).isEqualTo(7)
        assertThat(dto.calendarEndHour).isEqualTo(21)
        assertThat(dto.inviteCode).isEqualTo("invite123")
        assertThat(dto.inviteLink).isEqualTo("/register/invite123")
        assertThat(dto.adjacentBookingRequired).isFalse()
    }

    @Test
    fun `settings update delegates to service with authenticated admin id`() {
        val adminId = UUID.randomUUID()
        val request = UpdateAdminSettingsRequest(calendarStartHour = 8, calendarEndHour = 18)
        val expected = AdminSettingsDTO(8, 18, "code", "/register/code", true)
        every { adminSettingsService.updateSettings(adminId, request) } returns expected

        val dto = controller.updateSettings(principal(adminId), request).body!!

        assertThat(dto).isEqualTo(expected)
        verify(exactly = 1) { adminSettingsService.updateSettings(adminId, request) }
    }

    @Test
    fun `regenerating invite code saves new lowercase 16 character code`() {
        val adminId = UUID.randomUUID()
        every { userRepository.findById(adminId) } returns Optional.of(admin(adminId, inviteCode = "oldcode"))
        every { userRepository.save(any()) } answers { firstArg() }

        val dto = controller.regenerateInviteCode(principal(adminId)).body!!

        assertThat(dto.inviteCode).isNotEqualTo("oldcode")
        assertThat(dto.inviteCode).hasSize(16)
        assertThat(dto.inviteCode).matches("[a-z0-9]{16}")
        assertThat(dto.inviteLink).isEqualTo("/register/${dto.inviteCode}")
    }

    private fun principal(adminId: UUID) = UserPrincipal(
        userId = adminId.toString(),
        email = "admin@test.com",
        role = "admin"
    )

    private fun admin(id: UUID, inviteCode: String?) = User(
        id = id,
        email = "$id@test.com",
        passwordHash = "hash",
        role = "admin",
        calendarStartHour = 7,
        calendarEndHour = 21,
        inviteCode = inviteCode,
        adjacentBookingRequired = false
    )
}
