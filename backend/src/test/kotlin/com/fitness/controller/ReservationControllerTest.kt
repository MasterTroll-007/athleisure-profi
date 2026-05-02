package com.fitness.controller

import com.fitness.dto.AvailableSlotsResponse
import com.fitness.dto.CreateReservationRequest
import com.fitness.dto.ReservationDTO
import com.fitness.security.UserPrincipal
import com.fitness.service.AvailabilityService
import com.fitness.service.ReservationService
import com.fitness.service.WorkoutLogService
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import java.nio.charset.StandardCharsets
import java.util.UUID

class ReservationControllerTest {
    private val reservationService = mockk<ReservationService>()
    private val availabilityService = mockk<AvailabilityService>()
    private val workoutLogService = mockk<WorkoutLogService>()
    private val controller = ReservationController(reservationService, availabilityService, workoutLogService)

    @Test
    fun `available range rejects reversed or too wide requests`() {
        assertThatThrownBy {
            controller.getAvailableSlotsRange(principal(UUID.randomUUID()), "2026-05-10", "2026-05-01")
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("end")

        assertThatThrownBy {
            controller.getAvailableSlotsRange(principal(UUID.randomUUID()), "2026-05-01", "2026-09-01")
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("too wide")
    }

    @Test
    fun `available range delegates valid date window to availability service`() {
        val userId = UUID.randomUUID()
        every { availabilityService.getAvailableSlotsRange(any(), any(), userId.toString()) } returns emptyList()

        val response = controller.getAvailableSlotsRange(principal(userId), "2026-05-01", "2026-05-07")

        assertThat(response.body).isEqualTo(AvailableSlotsResponse(emptyList()))
    }

    @Test
    fun `create reservation returns created status with service response`() {
        val userId = UUID.randomUUID()
        val request = CreateReservationRequest(
            date = "2026-05-10",
            startTime = "10:00",
            endTime = "11:00",
            slotId = UUID.randomUUID().toString(),
            pricingItemId = null
        )
        val dto = reservationDto(userId = userId.toString())
        every { reservationService.createReservation(userId.toString(), request) } returns dto

        val response = controller.createReservation(principal(userId), request)

        assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
        assertThat(response.body).isEqualTo(dto)
    }

    @Test
    fun `reservation detail is forbidden for another client but allowed for admin`() {
        val ownerId = UUID.randomUUID()
        val otherId = UUID.randomUUID()
        val reservationId = UUID.randomUUID().toString()
        every { reservationService.getReservationById(reservationId) } returns reservationDto(userId = ownerId.toString())

        val forbidden = controller.getReservation(principal(otherId), reservationId)
        val admin = controller.getReservation(principal(otherId, role = "admin"), reservationId)

        assertThat(forbidden.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
        assertThat(admin.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(admin.body!!.userId).isEqualTo(ownerId.toString())
    }

    @Test
    fun `workout log ownership guard mirrors reservation ownership`() {
        val ownerId = UUID.randomUUID()
        val otherId = UUID.randomUUID()
        val reservationId = UUID.randomUUID().toString()
        every { reservationService.getReservationById(reservationId) } returns reservationDto(userId = ownerId.toString())

        val forbidden = controller.getWorkoutLog(principal(otherId), reservationId)

        assertThat(forbidden.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
    }

    @Test
    fun `ical export skips cancelled reservations and includes training summary`() {
        val userId = UUID.randomUUID()
        val active = reservationDto(
            id = "active-id",
            userId = userId.toString(),
            date = "2026-05-10",
            startTime = "10:00",
            endTime = "11:00",
            pricingItemName = "Strength"
        )
        val cancelled = active.copy(id = "cancelled-id", status = "cancelled")
        every { reservationService.getUpcomingReservations(userId.toString()) } returns listOf(active, cancelled)

        val response = controller.exportIcal(principal(userId))
        val ical = response.body!!.toString(StandardCharsets.UTF_8)

        assertThat(response.headers.contentType.toString()).isEqualTo("text/calendar")
        assertThat(ical).contains("BEGIN:VCALENDAR", "UID:active-id@rezervace-pankova.online", "Strength")
        assertThat(ical).doesNotContain("cancelled-id")
    }

    private fun principal(id: UUID, role: String = "client") = UserPrincipal(
        userId = id.toString(),
        email = "$id@test.com",
        role = role
    )

    private fun reservationDto(
        id: String = UUID.randomUUID().toString(),
        userId: String = UUID.randomUUID().toString(),
        date: String = "2026-05-10",
        startTime: String = "10:00",
        endTime: String = "11:00",
        status: String = "confirmed",
        pricingItemName: String? = null
    ) = ReservationDTO(
        id = id,
        userId = userId,
        userName = "Client",
        userEmail = "client@test.com",
        slotId = null,
        date = date,
        startTime = startTime,
        endTime = endTime,
        status = status,
        creditsUsed = 1,
        pricingItemId = null,
        pricingItemName = pricingItemName,
        createdAt = "2026-05-01T10:00:00Z",
        cancelledAt = null
    )
}
