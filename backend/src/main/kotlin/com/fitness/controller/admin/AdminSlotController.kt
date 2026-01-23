package com.fitness.controller.admin

import com.fitness.dto.*
import com.fitness.service.SlotService
import com.fitness.service.TemplateService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import java.util.*

@RestController
@RequestMapping("/api/admin/slots")
@PreAuthorize("hasRole('ADMIN')")
class AdminSlotController(
    private val slotService: SlotService,
    private val templateService: TemplateService
) {
    @GetMapping
    fun getSlots(
        @RequestParam start: String,
        @RequestParam end: String
    ): ResponseEntity<List<SlotDTO>> {
        val startDate = LocalDate.parse(start)
        val endDate = LocalDate.parse(end)
        val slots = slotService.getSlots(startDate, endDate)
        return ResponseEntity.ok(slots)
    }

    @PostMapping
    fun createSlot(@Valid @RequestBody request: CreateSlotRequest): ResponseEntity<Any> {
        return try {
            val slot = slotService.createSlot(request)
            ResponseEntity.status(HttpStatus.CREATED).body(slot)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(mapOf("error" to e.message))
        }
    }

    @PatchMapping("/{id}")
    fun updateSlot(
        @PathVariable id: String,
        @Valid @RequestBody request: UpdateSlotRequest
    ): ResponseEntity<Any> {
        return try {
            val slot = slotService.updateSlot(UUID.fromString(id), request)
            ResponseEntity.ok(slot)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(mapOf("error" to e.message))
        }
    }

    @DeleteMapping("/{id}")
    fun deleteSlot(@PathVariable id: String): ResponseEntity<Any> {
        return try {
            slotService.deleteSlot(UUID.fromString(id))
            ResponseEntity.ok(mapOf("message" to "Slot deleted"))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("error" to e.message))
        }
    }

    @PostMapping("/unlock-week")
    fun unlockWeek(@Valid @RequestBody request: UnlockWeekRequest): ResponseEntity<Map<String, Any>> {
        val weekStartDate = LocalDate.parse(request.weekStartDate)
        val count = slotService.unlockWeek(weekStartDate)
        return ResponseEntity.ok(mapOf("message" to "Week unlocked", "unlockedCount" to count))
    }

    @PostMapping("/apply-template")
    fun applyTemplate(@Valid @RequestBody request: ApplyTemplateRequest): ResponseEntity<Any> {
        return try {
            val templateId = UUID.fromString(request.templateId)
            val weekStartDate = LocalDate.parse(request.weekStartDate)
            val template = templateService.getTemplate(templateId)
            val slots = slotService.applyTemplate(templateId, weekStartDate, template.slots)
            ResponseEntity.ok(mapOf("message" to "Template applied", "createdSlots" to slots.size, "slots" to slots))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(mapOf("error" to e.message))
        }
    }
}
