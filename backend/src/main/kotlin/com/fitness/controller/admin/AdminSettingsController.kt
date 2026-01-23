package com.fitness.controller.admin

import com.fitness.dto.*
import com.fitness.mapper.UserMapper
import com.fitness.repository.ReservationRepository
import com.fitness.repository.UserRepository
import com.fitness.security.UserPrincipal
import com.fitness.service.CreditService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import java.util.*

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
class AdminSettingsController(
    private val userRepository: UserRepository,
    private val reservationRepository: ReservationRepository,
    private val creditService: CreditService,
    private val userMapper: UserMapper
) {
    @GetMapping("/settings")
    fun getSettings(@AuthenticationPrincipal principal: UserPrincipal): ResponseEntity<AdminSettingsDTO> {
        val admin = userRepository.findById(UUID.fromString(principal.userId))
            .orElseThrow { NoSuchElementException("Admin not found") }
        return ResponseEntity.ok(AdminSettingsDTO(
            calendarStartHour = admin.calendarStartHour,
            calendarEndHour = admin.calendarEndHour,
            inviteCode = admin.inviteCode,
            inviteLink = admin.inviteCode?.let { "/register/$it" }
        ))
    }

    @PatchMapping("/settings")
    fun updateSettings(
        @AuthenticationPrincipal principal: UserPrincipal,
        @Valid @RequestBody request: UpdateAdminSettingsRequest
    ): ResponseEntity<AdminSettingsDTO> {
        val admin = userRepository.findById(UUID.fromString(principal.userId))
            .orElseThrow { NoSuchElementException("Admin not found") }

        val startHour = request.calendarStartHour ?: admin.calendarStartHour
        val endHour = request.calendarEndHour ?: admin.calendarEndHour

        if (startHour >= endHour) {
            throw IllegalArgumentException("Start hour must be less than end hour")
        }

        val updated = admin.copy(
            calendarStartHour = startHour,
            calendarEndHour = endHour
        )
        val saved = userRepository.save(updated)

        return ResponseEntity.ok(AdminSettingsDTO(
            calendarStartHour = saved.calendarStartHour,
            calendarEndHour = saved.calendarEndHour,
            inviteCode = saved.inviteCode,
            inviteLink = saved.inviteCode?.let { "/register/$it" }
        ))
    }

    @PostMapping("/settings/regenerate-code")
    fun regenerateInviteCode(@AuthenticationPrincipal principal: UserPrincipal): ResponseEntity<AdminSettingsDTO> {
        val admin = userRepository.findById(UUID.fromString(principal.userId))
            .orElseThrow { NoSuchElementException("Admin not found") }

        // Generate new 16-character invite code
        val newCode = UUID.randomUUID().toString().replace("-", "").take(16).lowercase()

        val updated = admin.copy(inviteCode = newCode)
        val saved = userRepository.save(updated)

        return ResponseEntity.ok(AdminSettingsDTO(
            calendarStartHour = saved.calendarStartHour,
            calendarEndHour = saved.calendarEndHour,
            inviteCode = saved.inviteCode,
            inviteLink = saved.inviteCode?.let { "/register/$it" }
        ))
    }

    @GetMapping("/dashboard")
    fun getDashboard(): ResponseEntity<Map<String, Any>> {
        val totalClients = userRepository.countByRole("client")
        val today = LocalDate.now()
        val todayReservations = reservationRepository.countByDateRange(today, today)
        val weekReservations = reservationRepository.countByDateRange(today, today.plusDays(7))

        return ResponseEntity.ok(mapOf(
            "totalClients" to totalClients,
            "todayReservations" to todayReservations,
            "weekReservations" to weekReservations
        ))
    }

    @GetMapping("/trainers")
    fun getTrainers(): ResponseEntity<List<TrainerDTO>> {
        val trainers = userRepository.findByRole("admin").map { trainer ->
            userMapper.toTrainerDTO(trainer)
        }
        return ResponseEntity.ok(trainers)
    }

    @PostMapping("/credits/adjust")
    fun adjustCredits(
        @AuthenticationPrincipal principal: UserPrincipal,
        @Valid @RequestBody request: AdminAdjustCreditsRequest
    ): ResponseEntity<CreditBalanceResponse> {
        val balance = creditService.adjustCredits(principal.userId, request)
        return ResponseEntity.ok(balance)
    }
}
