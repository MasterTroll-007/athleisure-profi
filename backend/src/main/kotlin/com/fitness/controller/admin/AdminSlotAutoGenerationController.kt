package com.fitness.controller.admin

import com.fitness.dto.AutoGenerateSlotsRequest
import com.fitness.security.UserPrincipal
import com.fitness.service.SlotAutoGeneratorService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.UUID

@RestController
@RequestMapping("/api/admin/slots/auto-generate")
@PreAuthorize("hasRole('ADMIN')")
class AdminSlotAutoGenerationController(
    private val slotAutoGeneratorService: SlotAutoGeneratorService
) {
    @PostMapping
    fun autoGenerateSlots(
        @AuthenticationPrincipal principal: UserPrincipal,
        @Valid @RequestBody request: AutoGenerateSlotsRequest
    ): ResponseEntity<Map<String, Any>> {
        val mondayDate = LocalDate.parse(request.weekStartDate)
        val monday = if (mondayDate.dayOfWeek == DayOfWeek.MONDAY) {
            mondayDate
        } else {
            mondayDate.with(DayOfWeek.MONDAY)
        }

        val count = slotAutoGeneratorService.generateSlotsForWeek(monday, UUID.fromString(principal.userId))
        return ResponseEntity.ok(mapOf(
            "generated" to count as Any,
            "weekStartDate" to monday.toString() as Any
        ))
    }
}
