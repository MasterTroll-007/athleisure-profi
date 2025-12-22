package com.fitness.controller

import com.fitness.dto.*
import com.fitness.entity.AvailabilityBlock
import com.fitness.repository.AvailabilityBlockRepository
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.util.*

@RestController
@RequestMapping("/api/availability")
class AvailabilityController(
    private val availabilityBlockRepository: AvailabilityBlockRepository
) {

    @GetMapping("/blocks")
    @PreAuthorize("hasRole('ADMIN')")
    fun getAllBlocks(): ResponseEntity<List<AvailabilityBlockDTO>> {
        val blocks = availabilityBlockRepository.findAll().map { it.toDTO() }
        return ResponseEntity.ok(blocks)
    }

    @GetMapping("/blocks/active")
    fun getActiveBlocks(): ResponseEntity<List<AvailabilityBlockDTO>> {
        val blocks = availabilityBlockRepository.findByIsActiveTrue().map { it.toDTO() }
        return ResponseEntity.ok(blocks)
    }

    @GetMapping("/blocks/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    fun getBlock(@PathVariable id: String): ResponseEntity<AvailabilityBlockDTO> {
        val block = availabilityBlockRepository.findById(UUID.fromString(id))
            .orElseThrow { NoSuchElementException("Block not found") }
        return ResponseEntity.ok(block.toDTO())
    }

    @PostMapping("/blocks")
    @PreAuthorize("hasRole('ADMIN')")
    fun createBlock(@RequestBody request: CreateAvailabilityBlockRequest): ResponseEntity<AvailabilityBlockDTO> {
        val block = AvailabilityBlock(
            name = request.name,
            daysOfWeek = request.daysOfWeek.joinToString(","),
            dayOfWeek = request.dayOfWeek?.let { DayOfWeek.valueOf(it.uppercase()) },
            specificDate = request.specificDate?.let { LocalDate.parse(it) },
            startTime = LocalTime.parse(request.startTime),
            endTime = LocalTime.parse(request.endTime),
            slotDurationMinutes = request.slotDurationMinutes,
            isRecurring = request.isRecurring,
            isBlocked = request.isBlocked,
            isActive = true
        )
        val saved = availabilityBlockRepository.save(block)
        return ResponseEntity.status(HttpStatus.CREATED).body(saved.toDTO())
    }

    @PutMapping("/blocks/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    fun updateBlock(
        @PathVariable id: String,
        @RequestBody request: UpdateAvailabilityBlockRequest
    ): ResponseEntity<AvailabilityBlockDTO> {
        val existing = availabilityBlockRepository.findById(UUID.fromString(id))
            .orElseThrow { NoSuchElementException("Block not found") }

        val updated = existing.copy(
            name = request.name ?: existing.name,
            daysOfWeek = request.daysOfWeek?.joinToString(",") ?: existing.daysOfWeek,
            dayOfWeek = request.dayOfWeek?.let { DayOfWeek.valueOf(it.uppercase()) } ?: existing.dayOfWeek,
            specificDate = request.specificDate?.let { LocalDate.parse(it) } ?: existing.specificDate,
            startTime = request.startTime?.let { LocalTime.parse(it) } ?: existing.startTime,
            endTime = request.endTime?.let { LocalTime.parse(it) } ?: existing.endTime,
            slotDurationMinutes = request.slotDurationMinutes ?: existing.slotDurationMinutes,
            isRecurring = request.isRecurring ?: existing.isRecurring,
            isBlocked = request.isBlocked ?: existing.isBlocked,
            isActive = request.isActive ?: existing.isActive
        )

        val saved = availabilityBlockRepository.save(updated)
        return ResponseEntity.ok(saved.toDTO())
    }

    @DeleteMapping("/blocks/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    fun deleteBlock(@PathVariable id: String): ResponseEntity<Map<String, String>> {
        val uuid = UUID.fromString(id)
        if (!availabilityBlockRepository.existsById(uuid)) {
            throw NoSuchElementException("Block not found")
        }
        availabilityBlockRepository.deleteById(uuid)
        return ResponseEntity.ok(mapOf("message" to "Block deleted"))
    }

    private fun AvailabilityBlock.toDTO() = AvailabilityBlockDTO(
        id = id.toString(),
        name = name,
        daysOfWeek = if (daysOfWeek.isNotEmpty()) daysOfWeek.split(",").map { it.trim().toInt() } else emptyList(),
        dayOfWeek = dayOfWeek?.name,
        specificDate = specificDate?.toString(),
        startTime = startTime.toString(),
        endTime = endTime.toString(),
        slotDurationMinutes = slotDurationMinutes ?: slotDuration,
        isRecurring = isRecurring,
        isBlocked = isBlocked,
        isActive = isActive,
        createdAt = createdAt.toString()
    )
}
