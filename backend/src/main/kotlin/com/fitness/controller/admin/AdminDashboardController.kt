package com.fitness.controller.admin

import com.fitness.repository.ReservationRepository
import com.fitness.repository.UserRepository
import com.fitness.security.UserPrincipal
import com.fitness.service.CreditService
import com.fitness.service.FeedbackService
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
class AdminDashboardController(
    private val userRepository: UserRepository,
    private val reservationRepository: ReservationRepository,
    private val creditService: CreditService,
    private val feedbackService: FeedbackService
) {
    @GetMapping("/dashboard")
    fun getDashboard(@AuthenticationPrincipal principal: UserPrincipal): ResponseEntity<Map<String, Any>> {
        val adminId = UUID.fromString(principal.userId)
        val totalClients = userRepository.countClientsByTrainerId(adminId)
        val today = LocalDate.now()
        val todayReservations = reservationRepository.countByDateRangeForAdmin(today, today, adminId)
        val weekReservations = reservationRepository.countByDateRangeForAdmin(today, today.plusDays(7), adminId)

        return ResponseEntity.ok(mapOf(
            "totalClients" to totalClients,
            "todayReservations" to todayReservations,
            "weekReservations" to weekReservations
        ))
    }

    @GetMapping("/statistics")
    fun getStatistics(
        @AuthenticationPrincipal principal: UserPrincipal,
        @RequestParam(required = false) months: Int?
    ): ResponseEntity<Map<String, Any>> {
        val monthsBack = (months ?: 6).coerceIn(1, 24)
        val today = LocalDate.now()
        val adminId = UUID.fromString(principal.userId)
        val periodStart = today.minusMonths(monthsBack.toLong())

        val totalClients = userRepository.countClientsByTrainerId(adminId)
        val totalReservations = reservationRepository.countByDateRangeForAdmin(periodStart, today, adminId)
        val completedCount = reservationRepository.countByStatusAndDateBetweenForAdmin("completed", periodStart, today, adminId)
        val noShowCount = reservationRepository.countByStatusAndDateBetweenForAdmin("no_show", periodStart, today, adminId)
        val expiredConfirmed = reservationRepository.countByStatusAndDateBetweenForAdmin("confirmed", periodStart, today, adminId)
        val totalTracked = completedCount + noShowCount + expiredConfirmed
        val attendanceRate = if (totalTracked > 0) (completedCount * 100.0 / totalTracked).toLong() else 0L
        val noShowRate = if (totalTracked > 0) (noShowCount * 100.0 / totalTracked).toLong() else 0L
        val feedbackSummary = feedbackService.getTrainerFeedbackSummary(adminId)
        val zone = ZoneId.of("Europe/Prague")
        val instantStart = periodStart.atStartOfDay(zone).toInstant()
        val instantEnd = today.plusDays(1).atStartOfDay(zone).toInstant()
        val creditsSold = creditService.sumCreditsSoldInPeriod(instantStart, instantEnd, adminId)
        val monthlyStats = (0 until monthsBack).map { i ->
            val monthStart = today.minusMonths(i.toLong()).withDayOfMonth(1)
            val monthEnd = monthStart.plusMonths(1).minusDays(1)
            mapOf(
                "month" to monthStart.toString(),
                "reservations" to reservationRepository.countByDateRangeForAdmin(monthStart, monthEnd, adminId)
            )
        }.reversed()

        return ResponseEntity.ok(mapOf(
            "totalClients" to totalClients as Any,
            "totalReservations" to totalReservations as Any,
            "completedCount" to completedCount as Any,
            "noShowCount" to noShowCount as Any,
            "attendanceRate" to attendanceRate as Any,
            "noShowRate" to noShowRate as Any,
            "averageRating" to (feedbackSummary.averageRating ?: 0.0) as Any,
            "totalFeedback" to feedbackSummary.totalCount as Any,
            "ratingDistribution" to feedbackSummary.distribution as Any,
            "creditsSold" to creditsSold as Any,
            "monthlyStats" to monthlyStats as Any
        ))
    }
}
