package com.fitness.controller.admin

import com.fitness.dto.*
import com.fitness.repository.UserRepository
import com.fitness.security.UserPrincipal
import com.fitness.service.AdminSettingsService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
class AdminSettingsController(
    private val userRepository: UserRepository,
    private val adminSettingsService: AdminSettingsService
) {
    @GetMapping("/settings")
    fun getSettings(@AuthenticationPrincipal principal: UserPrincipal): ResponseEntity<AdminSettingsDTO> {
        val admin = userRepository.findById(UUID.fromString(principal.userId))
            .orElseThrow { NoSuchElementException("Admin not found") }
        return ResponseEntity.ok(AdminSettingsDTO(
            calendarStartHour = admin.calendarStartHour,
            calendarEndHour = admin.calendarEndHour,
            inviteCode = admin.inviteCode,
            inviteLink = admin.inviteCode?.let { "/register/$it" },
            adjacentBookingRequired = admin.adjacentBookingRequired
        ))
    }

    @PatchMapping("/settings")
    fun updateSettings(
        @AuthenticationPrincipal principal: UserPrincipal,
        @Valid @RequestBody request: UpdateAdminSettingsRequest
    ): ResponseEntity<AdminSettingsDTO> {
        val settings = adminSettingsService.updateSettings(UUID.fromString(principal.userId), request)
        return ResponseEntity.ok(settings)
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
            inviteLink = saved.inviteCode?.let { "/register/$it" },
            adjacentBookingRequired = saved.adjacentBookingRequired
        ))
    }
}
