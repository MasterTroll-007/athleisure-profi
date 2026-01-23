package com.fitness.controller.admin

import com.fitness.dto.*
import com.fitness.entity.AvailabilityBlock
import com.fitness.mapper.AvailabilityBlockMapper
import com.fitness.repository.AvailabilityBlockRepository
import com.fitness.security.UserPrincipal
import com.fitness.service.AvailabilityBlockValidationService
import com.fitness.service.AvailabilityService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.util.*

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
class AdminAvailabilityController(
    private val availabilityBlockRepository: AvailabilityBlockRepository,
    private val availabilityService: AvailabilityService,
    private val blockValidationService: AvailabilityBlockValidationService,
    private val availabilityBlockMapper: AvailabilityBlockMapper
) {
    @GetMapping("/blocks")
    fun getAllBlocks(): ResponseEntity<List<AvailabilityBlockDTO>> {
        val blocks = availabilityBlockRepository.findAll()
        return ResponseEntity.ok(availabilityBlockMapper.toDTOBatch(blocks))
    }

    @GetMapping("/blocks/{id}")
    fun getBlock(@PathVariable id: String): ResponseEntity<AvailabilityBlockDTO> {
        val block = availabilityBlockRepository.findById(UUID.fromString(id))
            .orElseThrow { NoSuchElementException("Block not found") }
        return ResponseEntity.ok(availabilityBlockMapper.toDTO(block))
    }

    @PostMapping("/blocks")
    fun createBlock(
        @AuthenticationPrincipal principal: UserPrincipal,
        @Valid @RequestBody request: CreateAvailabilityBlockRequest
    ): ResponseEntity<Any> {
        val adminId = UUID.fromString(principal.userId)
        val startTime = LocalTime.parse(request.startTime)
        val endTime = LocalTime.parse(request.endTime)

        // Check for overlapping blocks (only for non-blocked availability blocks)
        if (request.isBlocked != true) {
            val overlappingBlock = blockValidationService.checkForOverlappingBlocks(
                daysOfWeek = request.daysOfWeek,
                startTime = startTime,
                endTime = endTime,
                adminId = adminId
            )
            if (overlappingBlock != null) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(
                    blockValidationService.formatOverlapError(overlappingBlock)
                )
            }
        }

        val block = AvailabilityBlock(
            name = request.name,
            daysOfWeek = request.daysOfWeek.joinToString(","),
            dayOfWeek = request.dayOfWeek?.let { DayOfWeek.valueOf(it.uppercase()) },
            specificDate = request.specificDate?.let { LocalDate.parse(it) },
            startTime = startTime,
            endTime = endTime,
            slotDurationMinutes = request.slotDurationMinutes,
            isRecurring = request.isRecurring,
            isBlocked = request.isBlocked,
            isActive = true,
            adminId = adminId
        )
        val saved = availabilityBlockRepository.save(block)
        return ResponseEntity.status(HttpStatus.CREATED).body(availabilityBlockMapper.toDTO(saved))
    }

    @RequestMapping(value = ["/blocks/{id}"], method = [RequestMethod.PUT, RequestMethod.PATCH])
    fun updateBlock(
        @PathVariable id: String,
        @Valid @RequestBody request: UpdateAvailabilityBlockRequest
    ): ResponseEntity<Any> {
        val blockId = UUID.fromString(id)
        val existing = availabilityBlockRepository.findById(blockId)
            .orElseThrow { NoSuchElementException("Block not found") }

        val newStartTime = request.startTime?.let { LocalTime.parse(it) } ?: existing.startTime
        val newEndTime = request.endTime?.let { LocalTime.parse(it) } ?: existing.endTime
        val newDaysOfWeek = request.daysOfWeek ?: existing.daysOfWeek.split(",").mapNotNull { it.trim().toIntOrNull() }
        val newIsBlocked = request.isBlocked ?: existing.isBlocked

        // Check for overlapping blocks (only for non-blocked availability blocks)
        if (newIsBlocked != true) {
            val overlappingBlock = blockValidationService.checkForOverlappingBlocks(
                daysOfWeek = newDaysOfWeek,
                startTime = newStartTime,
                endTime = newEndTime,
                excludeBlockId = blockId,
                adminId = existing.adminId
            )
            if (overlappingBlock != null) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(
                    blockValidationService.formatOverlapError(overlappingBlock)
                )
            }
        }

        val updated = existing.copy(
            name = request.name ?: existing.name,
            daysOfWeek = request.daysOfWeek?.joinToString(",") ?: existing.daysOfWeek,
            dayOfWeek = request.dayOfWeek?.let { DayOfWeek.valueOf(it.uppercase()) } ?: existing.dayOfWeek,
            specificDate = request.specificDate?.let { LocalDate.parse(it) } ?: existing.specificDate,
            startTime = newStartTime,
            endTime = newEndTime,
            slotDurationMinutes = request.slotDurationMinutes ?: existing.slotDurationMinutes,
            isRecurring = request.isRecurring ?: existing.isRecurring,
            isBlocked = newIsBlocked,
            isActive = request.isActive ?: existing.isActive
        )

        val saved = availabilityBlockRepository.save(updated)
        return ResponseEntity.ok(availabilityBlockMapper.toDTO(saved))
    }

    @DeleteMapping("/blocks/{id}")
    fun deleteBlock(@PathVariable id: String): ResponseEntity<Map<String, String>> {
        val uuid = UUID.fromString(id)
        if (!availabilityBlockRepository.existsById(uuid)) {
            throw NoSuchElementException("Block not found")
        }
        availabilityBlockRepository.deleteById(uuid)
        return ResponseEntity.ok(mapOf("message" to "Block deleted"))
    }

    @GetMapping("/calendar/slots")
    fun getCalendarSlots(
        @RequestParam start: String,
        @RequestParam end: String
    ): ResponseEntity<List<AdminCalendarSlotDTO>> {
        val startDate = LocalDate.parse(start)
        val endDate = LocalDate.parse(end)
        val slots = availabilityService.getAdminCalendarSlots(startDate, endDate)
        return ResponseEntity.ok(slots)
    }
}
