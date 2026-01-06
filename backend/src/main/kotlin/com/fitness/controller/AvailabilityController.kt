package com.fitness.controller

import com.fitness.dto.*
import com.fitness.entity.AvailabilityBlock
import com.fitness.repository.AvailabilityBlockRepository
import com.fitness.repository.UserRepository
import com.fitness.security.UserPrincipal
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
@RequestMapping("/api/availability")
class AvailabilityController(
    private val availabilityBlockRepository: AvailabilityBlockRepository,
    private val userRepository: UserRepository
) {

    // Check if a new block overlaps with existing blocks
    private fun checkForOverlappingBlocks(
        daysOfWeek: List<Int>,
        startTime: LocalTime,
        endTime: LocalTime,
        excludeBlockId: UUID? = null
    ): AvailabilityBlock? {
        val existingBlocks = availabilityBlockRepository.findByIsActiveTrue()
            .filter { it.id != excludeBlockId }
            .filter { it.isBlocked != true }

        for (existingBlock in existingBlocks) {
            val timesOverlap = startTime < existingBlock.endTime && endTime > existingBlock.startTime
            if (!timesOverlap) continue

            val existingDays = if (existingBlock.daysOfWeek.isNotEmpty()) {
                existingBlock.daysOfWeek.split(",").mapNotNull { it.trim().toIntOrNull() }
            } else if (existingBlock.dayOfWeek != null) {
                listOf(existingBlock.dayOfWeek!!.value)
            } else {
                emptyList()
            }

            if (daysOfWeek.any { it in existingDays }) {
                return existingBlock
            }
        }
        return null
    }

    @GetMapping("/blocks")
    @PreAuthorize("hasRole('ADMIN')")
    fun getAllBlocks(@AuthenticationPrincipal principal: UserPrincipal): ResponseEntity<List<AvailabilityBlockDTO>> {
        val adminId = UUID.fromString(principal.userId)
        // Only return blocks created by this admin
        val blocks = availabilityBlockRepository.findByAdminId(adminId).map { it.toDTO() }
        return ResponseEntity.ok(blocks)
    }

    @GetMapping("/blocks/active")
    fun getActiveBlocks(@AuthenticationPrincipal principal: UserPrincipal): ResponseEntity<List<AvailabilityBlockDTO>> {
        val userId = UUID.fromString(principal.userId)
        val user = userRepository.findById(userId).orElseThrow { NoSuchElementException("User not found") }

        // If admin, show their own blocks; if client, show their trainer's blocks
        val trainerId = if (user.role == "admin") userId else user.trainerId
            ?: return ResponseEntity.ok(emptyList())

        val blocks = availabilityBlockRepository.findByIsActiveTrueAndAdminId(trainerId).map { it.toDTO() }
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
    fun createBlock(
        @AuthenticationPrincipal principal: UserPrincipal,
        @RequestBody request: CreateAvailabilityBlockRequest
    ): ResponseEntity<Any> {
        val adminId = UUID.fromString(principal.userId)
        val startTime = LocalTime.parse(request.startTime)
        val endTime = LocalTime.parse(request.endTime)

        if (request.isBlocked != true) {
            val overlappingBlock = checkForOverlappingBlocks(
                daysOfWeek = request.daysOfWeek,
                startTime = startTime,
                endTime = endTime
            )
            if (overlappingBlock != null) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(
                    mapOf(
                        "error" to "OVERLAPPING_BLOCK",
                        "message" to "Blok se překrývá s existujícím blokem '${overlappingBlock.name ?: "Bez názvu"}' (${overlappingBlock.startTime}-${overlappingBlock.endTime})"
                    )
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
        return ResponseEntity.status(HttpStatus.CREATED).body(saved.toDTO())
    }

    @PutMapping("/blocks/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    fun updateBlock(
        @PathVariable id: String,
        @RequestBody request: UpdateAvailabilityBlockRequest
    ): ResponseEntity<Any> {
        val blockId = UUID.fromString(id)
        val existing = availabilityBlockRepository.findById(blockId)
            .orElseThrow { NoSuchElementException("Block not found") }

        val newStartTime = request.startTime?.let { LocalTime.parse(it) } ?: existing.startTime
        val newEndTime = request.endTime?.let { LocalTime.parse(it) } ?: existing.endTime
        val newDaysOfWeek = request.daysOfWeek ?: existing.daysOfWeek.split(",").mapNotNull { it.trim().toIntOrNull() }
        val newIsBlocked = request.isBlocked ?: existing.isBlocked

        if (newIsBlocked != true) {
            val overlappingBlock = checkForOverlappingBlocks(
                daysOfWeek = newDaysOfWeek,
                startTime = newStartTime,
                endTime = newEndTime,
                excludeBlockId = blockId
            )
            if (overlappingBlock != null) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(
                    mapOf(
                        "error" to "OVERLAPPING_BLOCK",
                        "message" to "Blok se překrývá s existujícím blokem '${overlappingBlock.name ?: "Bez názvu"}' (${overlappingBlock.startTime}-${overlappingBlock.endTime})"
                    )
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

    @GetMapping("/calendar-settings")
    fun getCalendarSettings(@AuthenticationPrincipal principal: UserPrincipal): ResponseEntity<AdminSettingsDTO> {
        val user = userRepository.findById(UUID.fromString(principal.userId))
            .orElseThrow { NoSuchElementException("User not found") }

        // If user is admin, return their own settings
        if (user.role == "admin") {
            return ResponseEntity.ok(AdminSettingsDTO(
                calendarStartHour = user.calendarStartHour,
                calendarEndHour = user.calendarEndHour,
                inviteCode = null,
                inviteLink = null
            ))
        }

        // If user is client, return trainer's settings (or defaults if no trainer)
        val trainer = user.trainerId?.let { userRepository.findById(it).orElse(null) }
        return ResponseEntity.ok(AdminSettingsDTO(
            calendarStartHour = trainer?.calendarStartHour ?: 6,
            calendarEndHour = trainer?.calendarEndHour ?: 22,
            inviteCode = null,
            inviteLink = null
        ))
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
