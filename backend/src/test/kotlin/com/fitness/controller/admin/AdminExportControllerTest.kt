package com.fitness.controller.admin

import com.fitness.dto.ReservationDTO
import com.fitness.entity.Reservation
import com.fitness.entity.User
import com.fitness.mapper.PaymentMapper
import com.fitness.mapper.ReservationMapper
import com.fitness.repository.ReservationRepository
import com.fitness.repository.StripePaymentRepository
import com.fitness.repository.UserRepository
import com.fitness.security.UserPrincipal
import com.fitness.service.CsvExportService
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.http.HttpHeaders
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

class AdminExportControllerTest {
    private val userRepository = mockk<UserRepository>()
    private val reservationRepository = mockk<ReservationRepository>()
    private val stripePaymentRepository = mockk<StripePaymentRepository>()
    private val paymentMapper = mockk<PaymentMapper>()
    private val reservationMapper = mockk<ReservationMapper>()
    private val controller = AdminExportController(
        userRepository,
        reservationRepository,
        stripePaymentRepository,
        paymentMapper,
        reservationMapper,
        CsvExportService()
    )

    @Test
    fun `client export is trainer scoped and produces csv attachment`() {
        val adminId = UUID.randomUUID()
        val clientId = UUID.randomUUID()
        every { userRepository.findClientsByTrainerId(adminId, any()) } returns PageImpl(
            listOf(
                User(
                    id = clientId,
                    email = "client@test.com",
                    passwordHash = "hash",
                    firstName = "=Formula",
                    lastName = "Client",
                    phone = "123",
                    credits = 5
                )
            )
        )

        val response = controller.exportClients(principal(adminId))
        val csv = response.body!!.toString(StandardCharsets.UTF_8)

        assertThat(response.headers.getFirst(HttpHeaders.CONTENT_DISPOSITION)).isEqualTo("attachment; filename=\"clients.csv\"")
        assertThat(response.headers.contentType.toString()).isEqualTo("text/csv;charset=UTF-8")
        assertThat(csv).contains("id,email,first_name,last_name,phone,credits,created_at")
        assertThat(csv).contains(clientId.toString(), "client@test.com", "'=Formula", "5")
    }

    @Test
    fun `reservation export respects requested date range and mapped training type`() {
        val adminId = UUID.randomUUID()
        val reservation = Reservation(
            id = UUID.randomUUID(),
            userId = UUID.randomUUID(),
            date = LocalDate.of(2026, 5, 10),
            startTime = LocalTime.of(10, 0),
            endTime = LocalTime.of(11, 0),
            creditsUsed = 2
        )
        every {
            reservationRepository.findByDateRangeForAdmin(
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 31),
                adminId
            )
        } returns listOf(reservation)
        every { reservationMapper.toDTOBatch(listOf(reservation)) } returns listOf(
            ReservationDTO(
                id = reservation.id.toString(),
                userId = reservation.userId.toString(),
                userName = "Client One",
                userEmail = "client@test.com",
                slotId = null,
                date = "2026-05-10",
                startTime = "10:00",
                endTime = "11:00",
                status = "confirmed",
                creditsUsed = 2,
                pricingItemId = null,
                pricingItemName = "Personal training",
                createdAt = "2026-05-01T10:00:00Z",
                cancelledAt = null
            )
        )

        val response = controller.exportReservations(principal(adminId), "2026-05-01", "2026-05-31")
        val csv = response.body!!.toString(StandardCharsets.UTF_8)

        assertThat(response.headers.getFirst(HttpHeaders.CONTENT_DISPOSITION)).isEqualTo("attachment; filename=\"reservations.csv\"")
        assertThat(csv).contains("Client One", "client@test.com", "Personal training", "2")
    }

    @Test
    fun `payment export handles empty trainer payment list`() {
        val adminId = UUID.randomUUID()
        every { stripePaymentRepository.findByTrainerIdOrderByCreatedAtDesc(adminId) } returns emptyList()
        every { paymentMapper.toAdminDTOBatch(emptyList()) } returns emptyList()

        val response = controller.exportPayments(principal(adminId))
        val csv = response.body!!.toString(StandardCharsets.UTF_8)

        assertThat(response.headers.getFirst(HttpHeaders.CONTENT_DISPOSITION)).isEqualTo("attachment; filename=\"payments.csv\"")
        assertThat(csv).contains("id,client,amount,currency,state,package,created_at,stripe_session_id")
    }

    private fun principal(adminId: UUID) = UserPrincipal(
        userId = adminId.toString(),
        email = "admin@test.com",
        role = "admin"
    )
}
