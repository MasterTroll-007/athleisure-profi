package com.fitness.controller.admin

import com.fitness.dto.FeedbackSummaryDTO
import com.fitness.repository.ReservationRepository
import com.fitness.repository.UserRepository
import com.fitness.security.UserPrincipal
import com.fitness.service.CreditService
import com.fitness.service.FeedbackService
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class AdminDashboardControllerTest {
    private val userRepository = mockk<UserRepository>()
    private val reservationRepository = mockk<ReservationRepository>()
    private val creditService = mockk<CreditService>()
    private val feedbackService = mockk<FeedbackService>()
    private val controller = AdminDashboardController(
        userRepository,
        reservationRepository,
        creditService,
        feedbackService
    )

    @Test
    fun `dashboard returns admin scoped client and reservation counts`() {
        val adminId = UUID.randomUUID()
        val principal = principal(adminId)
        every { userRepository.countClientsByTrainerId(adminId) } returns 5
        every { reservationRepository.countByDateRangeForAdmin(any(), any(), adminId) } returnsMany listOf(2, 9)

        val body = controller.getDashboard(principal).body!!

        assertThat(body["totalClients"]).isEqualTo(5L)
        assertThat(body["todayReservations"]).isEqualTo(2L)
        assertThat(body["weekReservations"]).isEqualTo(9L)
    }

    @Test
    fun `statistics calculates attendance no show credits and monthly buckets`() {
        val adminId = UUID.randomUUID()
        val principal = principal(adminId)
        every { userRepository.countClientsByTrainerId(adminId) } returns 7
        every { reservationRepository.countByDateRangeForAdmin(any<LocalDate>(), any<LocalDate>(), adminId) } returns 10
        every {
            reservationRepository.countByStatusAndDateBetweenForAdmin("completed", any(), any(), adminId)
        } returns 6
        every {
            reservationRepository.countByStatusAndDateBetweenForAdmin("no_show", any(), any(), adminId)
        } returns 2
        every {
            reservationRepository.countByStatusAndDateBetweenForAdmin("confirmed", any(), any(), adminId)
        } returns 2
        every { feedbackService.getTrainerFeedbackSummary(adminId) } returns FeedbackSummaryDTO(
            averageRating = 4.5,
            totalCount = 3,
            distribution = mapOf(5 to 2, 4 to 1)
        )
        every { creditService.sumCreditsSoldInPeriod(any<Instant>(), any<Instant>(), adminId) } returns 42

        val body = controller.getStatistics(principal, months = 2).body!!

        assertThat(body["totalClients"]).isEqualTo(7L)
        assertThat(body["totalReservations"]).isEqualTo(10L)
        assertThat(body["completedCount"]).isEqualTo(6L)
        assertThat(body["noShowCount"]).isEqualTo(2L)
        assertThat(body["attendanceRate"]).isEqualTo(60L)
        assertThat(body["noShowRate"]).isEqualTo(20L)
        assertThat(body["averageRating"]).isEqualTo(4.5)
        assertThat(body["totalFeedback"]).isEqualTo(3L)
        assertThat(body["creditsSold"]).isEqualTo(42L)
        assertThat(body["monthlyStats"] as List<*>).hasSize(2)
    }

    private fun principal(adminId: UUID) = UserPrincipal(
        userId = adminId.toString(),
        email = "admin@test.com",
        role = "admin"
    )
}
