package com.fitness.controller.admin

import com.fitness.dto.*
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
    private val auditService: AuditService
) {
    @GetMapping
    fun getSlots(
        @AuthenticationPrincipal principal: UserPrincipal,
        @RequestParam start: String,
        @RequestParam end: String
    ): ResponseEntity<List<SlotDTO>> {
        val startDate = LocalDate.parse(start)
        val endDate = LocalDate.parse(end)
        val slots = slotService.getSlots(startDate, endDate, UUID.fromString(principal.userId))
        return ResponseEntity.ok(slots)
    }

    @PostMapping
    fun createSlot(
        @AuthenticationPrincipal principal: UserPrincipal,
        @Valid @RequestBody request: CreateSlotRequest
    ): ResponseEntity<Any> {
        return try {
            val slot = slotService.createSlot(request, UUID.fromString(principal.userId))
            auditService.logSlotCreated(principal.userId, principal.email, slot.id, slot.date, slot.startTime)
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
            val slot = slotService.updateSlot(UUID.fromString(id), request, UUID.fromString(principal.userId))
            auditService.logSlotUpdated(principal.userId, principal.email, id, mapOf(
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
    fun getSlotCancellationPreview(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable id: String
    ): ResponseEntity<Any> {
        return try {
            val preview = slotService.getSlotCancellationPreview(UUID.fromString(id), UUID.fromString(principal.userId))
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
            slotService.deleteSlot(UUID.fromString(id), UUID.fromString(principal.userId))
            auditService.logSlotDeleted(principal.userId, principal.email, id)
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
        val count = slotService.unlockWeek(weekStartDate, endDate, UUID.fromString(principal.userId))
        auditService.logSlotUnlocked(principal.userId, principal.email, count, request.weekStartDate)
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
            val adminId = UUID.fromString(principal.userId)
            val template = templateService.getTemplate(templateId, adminId)
            val templateLocationId = template.locationId?.let { UUID.fromString(it) }
            val slots = slotService.applyTemplate(templateId, weekStartDate, template.slots, templateLocationId, adminId)
            auditService.logTemplateApplied(principal.userId, principal.email, request.templateId, request.weekStartDate, slots.size)
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
        val adminId = UUID.fromString(principal.userId)
        for (slotId in request.slotIds) {
            try {
                slotService.deleteSlot(UUID.fromString(slotId), adminId)
                deleted++
            } catch (_: IllegalArgumentException) { }
        }
        auditService.logSlotDeleted(principal.userId, principal.email, "bulk:$deleted")
        return ResponseEntity.ok(mapOf("deleted" to deleted as Any))
    }

    @PostMapping("/bulk-update")
    fun bulkUpdateSlots(
        @AuthenticationPrincipal principal: UserPrincipal,
        @Valid @RequestBody request: BulkSlotUpdateRequest
    ): ResponseEntity<Map<String, Any>> {
        var updated = 0
        val adminId = UUID.fromString(principal.userId)
        for (slotId in request.slotIds) {
            try {
                slotService.updateSlot(UUID.fromString(slotId), UpdateSlotRequest(status = request.status), adminId)
                updated++
            } catch (_: IllegalArgumentException) { }
        }
        return ResponseEntity.ok(mapOf("updated" to updated as Any))
    }
}
