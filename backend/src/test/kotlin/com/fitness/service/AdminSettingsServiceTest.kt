package com.fitness.service

import com.fitness.dto.UpdateAdminSettingsRequest
import com.fitness.entity.User
import com.fitness.repository.UserRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.util.Optional
import java.util.UUID

class AdminSettingsServiceTest {
    private val userRepository = mockk<UserRepository>()
    private val slotService = mockk<SlotService>(relaxed = true)
    private val service = AdminSettingsService(userRepository, slotService)

    @Test
    fun `updating non calendar settings does not cleanup slots`() {
        val adminId = UUID.randomUUID()
        val admin = admin(adminId, startHour = 6, endHour = 22, adjacentBookingRequired = true)
        every { userRepository.findById(adminId) } returns Optional.of(admin)
        every { userRepository.save(any()) } answers { firstArg() }

        val dto = service.updateSettings(adminId, UpdateAdminSettingsRequest(adjacentBookingRequired = false))

        assertThat(dto.calendarStartHour).isEqualTo(6)
        assertThat(dto.calendarEndHour).isEqualTo(22)
        assertThat(dto.adjacentBookingRequired).isFalse()
        assertThat(dto.inviteLink).isEqualTo("/register/ABC123")
        verify(exactly = 0) { slotService.deleteFutureSlotsOutsideCalendarRange(any(), any(), any()) }
    }

    @Test
    fun `updating calendar range cleans hidden future slots`() {
        val adminId = UUID.randomUUID()
        val admin = admin(adminId, startHour = 6, endHour = 22)
        every { userRepository.findById(adminId) } returns Optional.of(admin)
        every { userRepository.save(any()) } answers { firstArg() }
        every { slotService.deleteFutureSlotsOutsideCalendarRange(adminId, 8, 18) } returns
            CalendarRangeCleanupResult(deletedSlots = 2, cancelledReservations = 1, refundedCredits = 3)

        val dto = service.updateSettings(
            adminId,
            UpdateAdminSettingsRequest(calendarStartHour = 8, calendarEndHour = 18)
        )

        assertThat(dto.calendarStartHour).isEqualTo(8)
        assertThat(dto.calendarEndHour).isEqualTo(18)
        verify(exactly = 1) { slotService.deleteFutureSlotsOutsideCalendarRange(adminId, 8, 18) }
    }

    @Test
    fun `calendar start must be before end`() {
        val adminId = UUID.randomUUID()
        every { userRepository.findById(adminId) } returns Optional.of(admin(adminId, startHour = 6, endHour = 22))

        assertThatThrownBy {
            service.updateSettings(
                adminId,
                UpdateAdminSettingsRequest(calendarStartHour = 18, calendarEndHour = 18)
            )
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("Start hour")

        verify(exactly = 0) { userRepository.save(any()) }
    }

    private fun admin(
        id: UUID,
        startHour: Int,
        endHour: Int,
        adjacentBookingRequired: Boolean = true
    ) = User(
        id = id,
        email = "$id@test.com",
        passwordHash = "hash",
        role = "admin",
        calendarStartHour = startHour,
        calendarEndHour = endHour,
        inviteCode = "ABC123",
        adjacentBookingRequired = adjacentBookingRequired
    )
}
