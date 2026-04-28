package com.fitness.controller.admin

import com.fitness.mapper.PaymentMapper
import com.fitness.mapper.ReservationMapper
import com.fitness.repository.ReservationRepository
import com.fitness.repository.StripePaymentRepository
import com.fitness.repository.UserRepository
import com.fitness.security.UserPrincipal
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.util.UUID

@RestController
@RequestMapping("/api/admin/export")
@PreAuthorize("hasRole('ADMIN')")
class AdminExportController(
    private val userRepository: UserRepository,
    private val reservationRepository: ReservationRepository,
    private val stripePaymentRepository: StripePaymentRepository,
    private val paymentMapper: PaymentMapper,
    private val reservationMapper: ReservationMapper
) {
    @GetMapping("/clients")
    fun exportClients(@AuthenticationPrincipal principal: UserPrincipal): ResponseEntity<ByteArray> {
        val adminId = UUID.fromString(principal.userId)
        val clients = userRepository.findClientsByTrainerId(
            adminId,
            PageRequest.of(0, 10000, Sort.by("createdAt").descending())
        ).content

        val rows = listOf(listOf("id", "email", "first_name", "last_name", "phone", "credits", "created_at")) +
            clients.map {
                listOf(
                    it.id.toString(),
                    it.email,
                    it.firstName.orEmpty(),
                    it.lastName.orEmpty(),
                    it.phone.orEmpty(),
                    it.credits.toString(),
                    it.createdAt.toString()
                )
            }
        return csvResponse("clients.csv", rows)
    }

    @GetMapping("/reservations")
    fun exportReservations(
        @AuthenticationPrincipal principal: UserPrincipal,
        @RequestParam(required = false) start: String?,
        @RequestParam(required = false) end: String?
    ): ResponseEntity<ByteArray> {
        val adminId = UUID.fromString(principal.userId)
        val startDate = start?.let { LocalDate.parse(it) } ?: LocalDate.now().minusMonths(12)
        val endDate = end?.let { LocalDate.parse(it) } ?: LocalDate.now().plusMonths(3)
        val reservations = reservationRepository.findByDateRangeForAdmin(startDate, endDate, adminId)
        val dtos = reservationMapper.toDTOBatch(reservations)

        val rows = listOf(listOf("id", "client", "email", "date", "start", "end", "status", "credits", "training_type")) +
            dtos.map {
                listOf(
                    it.id,
                    it.userName.orEmpty(),
                    it.userEmail.orEmpty(),
                    it.date,
                    it.startTime,
                    it.endTime,
                    it.status,
                    it.creditsUsed.toString(),
                    it.pricingItemName.orEmpty()
                )
            }
        return csvResponse("reservations.csv", rows)
    }

    @GetMapping("/payments")
    fun exportPayments(@AuthenticationPrincipal principal: UserPrincipal): ResponseEntity<ByteArray> {
        val payments = stripePaymentRepository.findByTrainerIdOrderByCreatedAtDesc(UUID.fromString(principal.userId))
        val dtos = paymentMapper.toAdminDTOBatch(payments)

        val rows = listOf(listOf("id", "client", "amount", "currency", "state", "package", "created_at", "stripe_session_id")) +
            dtos.map {
                listOf(
                    it.id,
                    it.userName.orEmpty(),
                    it.amount.toPlainString(),
                    it.currency,
                    it.state,
                    it.creditPackageName.orEmpty(),
                    it.createdAt,
                    it.stripeSessionId.orEmpty()
                )
            }
        return csvResponse("payments.csv", rows)
    }

    private fun csvResponse(filename: String, rows: List<List<String>>): ResponseEntity<ByteArray> {
        val csv = rows.joinToString("\n") { row -> row.joinToString(",") { csvEscape(it) } } + "\n"
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"$filename\"")
            .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
            .body(csv.toByteArray(Charsets.UTF_8))
    }

    private fun csvEscape(value: String): String {
        val normalized = value.replace("\r\n", "\n").replace("\r", "\n")
        return if (normalized.any { it == ',' || it == '"' || it == '\n' }) {
            "\"" + normalized.replace("\"", "\"\"") + "\""
        } else {
            normalized
        }
    }
}
