package com.fitness.controller

import com.fitness.dto.*
import com.fitness.repository.UserRepository
import com.fitness.security.UserPrincipal
import com.fitness.service.CreditService
import com.fitness.service.ReservationService
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
class AdminController(
    private val userRepository: UserRepository,
    private val reservationService: ReservationService,
    private val creditService: CreditService
) {

    @GetMapping("/clients")
    fun getClients(): ResponseEntity<List<UserDTO>> {
        val clients = userRepository.findByRole("client").map { user ->
            UserDTO(
                id = user.id.toString(),
                email = user.email,
                firstName = user.firstName,
                lastName = user.lastName,
                phone = user.phone,
                role = user.role,
                credits = user.credits,
                locale = user.locale,
                theme = user.theme,
                createdAt = user.createdAt.toString()
            )
        }
        return ResponseEntity.ok(clients)
    }

    @GetMapping("/clients/search")
    fun searchClients(@RequestParam q: String): ResponseEntity<List<UserDTO>> {
        val clients = userRepository.searchClients(q).map { user ->
            UserDTO(
                id = user.id.toString(),
                email = user.email,
                firstName = user.firstName,
                lastName = user.lastName,
                phone = user.phone,
                role = user.role,
                credits = user.credits,
                locale = user.locale,
                theme = user.theme,
                createdAt = user.createdAt.toString()
            )
        }
        return ResponseEntity.ok(clients)
    }

    @GetMapping("/reservations")
    fun getReservations(
        @RequestParam(required = false) start: String?,
        @RequestParam(required = false) end: String?
    ): ResponseEntity<List<ReservationCalendarEvent>> {
        val startDate = start?.let { LocalDate.parse(it) } ?: LocalDate.now().minusMonths(1)
        val endDate = end?.let { LocalDate.parse(it) } ?: LocalDate.now().plusMonths(2)
        val reservations = reservationService.getAllReservations(startDate, endDate)
        return ResponseEntity.ok(reservations)
    }

    @PostMapping("/credits/adjust")
    fun adjustCredits(
        @AuthenticationPrincipal principal: UserPrincipal,
        @RequestBody request: AdminAdjustCreditsRequest
    ): ResponseEntity<CreditBalanceResponse> {
        val balance = creditService.adjustCredits(principal.userId, request)
        return ResponseEntity.ok(balance)
    }

    @GetMapping("/dashboard")
    fun getDashboard(): ResponseEntity<Map<String, Any>> {
        val totalClients = userRepository.findByRole("client").size
        val today = LocalDate.now()
        val todayReservations = reservationService.getAllReservations(today, today).size
        val weekReservations = reservationService.getAllReservations(today, today.plusDays(7)).size

        return ResponseEntity.ok(mapOf(
            "totalClients" to totalClients,
            "todayReservations" to todayReservations,
            "weekReservations" to weekReservations
        ))
    }
}
