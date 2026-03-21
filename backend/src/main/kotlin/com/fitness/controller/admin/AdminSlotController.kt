package com.fitness.controller.admin

import com.fitness.dto.*
import com.fitness.repository.UserRepository
import com.fitness.security.UserPrincipal
import com.fitness.service.AuditService
import com.fitness.service.SlotService
import com.fitness.service.TemplateService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import java.util.*

@RestController
@RequestMapping("/api/admin/slots")
@PreAuthorize("hasRole('ADMIN')")
class AdminSlotController(
    private val slotService: SlotService,
    private val templateService: TemplateService,
    private val auditService: AuditService,
    private val userRepository: UserRepository
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
    fun createSlot(
        @AuthenticationPrincipal principal: UserPrincipal,
        @Valid @RequestBody request: CreateSlotRequest
    ): ResponseEntity<Any> {
        return try {
            val slot = slotService.createSlot(request)
            val admin = userRepository.findById(UUID.fromString(principal.userId)).orElse(null)
            auditService.logSlotCreated(principal.userId, admin?.email, slot.id, slot.date, slot.startTime)
            ResponseEntity.status(HttpStatus.CREATED).body(slot)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(mapOf("error" to e.message))
        }
    }

    @PatchMapping("/{id}")
    fun updateSlot(
        @PathVariable id: String,
        @AuthenticationPrincipal principal: UserPrincipal,
        @Valid @RequestBody request: UpdateSlotRequest
    ): ResponseEntity<Any> {
        return try {
            val slot = slotService.updateSlot(UUID.fromString(id), request)
            val admin = userRepository.findById(UUID.fromString(principal.userId)).orElse(null)
            auditService.logSlotUpdated(principal.userId, admin?.email, id, mapOf(
                "status" to request.status,
                "note" to request.note,
                "assignedUserId" to request.assignedUserId
            ))
            ResponseEntity.ok(slot)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(mapOf("error" to e.message))
        }
    }

    @GetMapping("/{id}/cancellation-preview")
    fun getSlotCancellationPreview(@PathVariable id: String): ResponseEntity<Any> {
        return try {
            val preview = slotService.getSlotCancellationPreview(UUID.fromString(id))
            ResponseEntity.ok(preview)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("error" to e.message))
        }
    }

    @DeleteMapping("/{id}")
    fun deleteSlot(
        @PathVariable id: String,
        @AuthenticationPrincipal principal: UserPrincipal
    ): ResponseEntity<Any> {
        return try {
            slotService.deleteSlot(UUID.fromString(id))
            val admin = userRepository.findById(UUID.fromString(principal.userId)).orElse(null)
            auditService.logSlotDeleted(principal.userId, admin?.email, id)
            ResponseEntity.ok(mapOf("message" to "Slot deleted"))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("error" to e.message))
        }
    }

    @PostMapping("/unlock-week")
    fun unlockWeek(
        @AuthenticationPrincipal principal: UserPrincipal,
        @Valid @RequestBody request: UnlockWeekRequest
    ): ResponseEntity<Map<String, Any>> {
        val weekStartDate = LocalDate.parse(request.weekStartDate)
        val endDate = request.endDate?.let { LocalDate.parse(it) }
        val count = slotService.unlockWeek(weekStartDate, endDate)
        val admin = userRepository.findById(UUID.fromString(principal.userId)).orElse(null)
        auditService.logSlotUnlocked(principal.userId, admin?.email, count, request.weekStartDate)
        return ResponseEntity.ok(mapOf("message" to "Week unlocked", "unlockedCount" to count))
    }

    @PostMapping("/apply-template")
    fun applyTemplate(
        @AuthenticationPrincipal principal: UserPrincipal,
        @Valid @RequestBody request: ApplyTemplateRequest
    ): ResponseEntity<Any> {
        return try {
            val templateId = UUID.fromString(request.templateId)
            val weekStartDate = LocalDate.parse(request.weekStartDate)
            val template = templateService.getTemplate(templateId)
            val slots = slotService.applyTemplate(templateId, weekStartDate, template.slots)
            val admin = userRepository.findById(UUID.fromString(principal.userId)).orElse(null)
            auditService.logTemplateApplied(principal.userId, admin?.email, request.templateId, request.weekStartDate, slots.size)
            ResponseEntity.ok(mapOf("message" to "Template applied", "createdSlots" to slots.size, "slots" to slots))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(mapOf("error" to e.message))
        }
    }

    @PostMapping("/bulk-delete")
    fun bulkDeleteSlots(
        @AuthenticationPrincipal principal: UserPrincipal,
        @Valid @RequestBody request: BulkSlotRequest
    ): ResponseEntity<Map<String, Any>> {
        var deleted = 0
        for (slotId in request.slotIds) {
            try {
                slotService.deleteSlot(UUID.fromString(slotId))
                deleted++
            } catch (_: IllegalArgumentException) { }
        }
        val admin = userRepository.findById(UUID.fromString(principal.userId)).orElse(null)
        auditService.logSlotDeleted(principal.userId, admin?.email, "bulk:$deleted")
        return ResponseEntity.ok(mapOf("deleted" to deleted as Any))
    }

    @PostMapping("/bulk-update")
    fun bulkUpdateSlots(
        @AuthenticationPrincipal principal: UserPrincipal,
        @Valid @RequestBody request: BulkSlotUpdateRequest
    ): ResponseEntity<Map<String, Any>> {
        var updated = 0
        for (slotId in request.slotIds) {
            try {
                slotService.updateSlot(UUID.fromString(slotId), UpdateSlotRequest(status = request.status))
                updated++
            } catch (_: IllegalArgumentException) { }
        }
        return ResponseEntity.ok(mapOf("updated" to updated as Any))
    }
}
