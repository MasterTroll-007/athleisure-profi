package com.fitness.controller

import com.fitness.dto.*
import com.fitness.entity.AvailabilityBlock
import com.fitness.entity.TrainingPlan
import com.fitness.repository.AvailabilityBlockRepository
import com.fitness.repository.TrainingPlanRepository
import com.fitness.repository.UserRepository
import com.fitness.security.UserPrincipal
import com.fitness.service.AvailabilityService
import com.fitness.service.CreditService
import com.fitness.service.ReservationService
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
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
class AdminController(
    private val userRepository: UserRepository,
    private val reservationService: ReservationService,
    private val creditService: CreditService,
    private val availabilityBlockRepository: AvailabilityBlockRepository,
    private val trainingPlanRepository: TrainingPlanRepository,
    private val availabilityService: AvailabilityService
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
            .filter { it.isBlocked != true } // Only check non-blocked availability blocks

        for (existingBlock in existingBlocks) {
            // Check time overlap: startA < endB AND endA > startB
            val timesOverlap = startTime < existingBlock.endTime && endTime > existingBlock.startTime

            if (!timesOverlap) continue

            // Check day overlap
            val existingDays = if (existingBlock.daysOfWeek.isNotEmpty()) {
                existingBlock.daysOfWeek.split(",").mapNotNull { it.trim().toIntOrNull() }
            } else if (existingBlock.dayOfWeek != null) {
                listOf(existingBlock.dayOfWeek!!.value)
            } else {
                emptyList()
            }

            val daysOverlap = daysOfWeek.any { it in existingDays }

            if (daysOverlap) {
                return existingBlock
            }
        }

        return null
    }

    @GetMapping("/clients")
    fun getClients(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<PageDTO<UserDTO>> {
        val pageable = PageRequest.of(page, size, Sort.by("createdAt").descending())
        val clientsPage = userRepository.findByRole("client", pageable)

        val pageDTO = PageDTO(
            content = clientsPage.content.map { user ->
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
            },
            totalElements = clientsPage.totalElements,
            totalPages = clientsPage.totalPages,
            page = clientsPage.number,
            size = clientsPage.size,
            hasNext = clientsPage.hasNext(),
            hasPrevious = clientsPage.hasPrevious()
        )
        return ResponseEntity.ok(pageDTO)
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

    @GetMapping("/clients/{id}")
    fun getClient(@PathVariable id: String): ResponseEntity<UserDTO> {
        val user = userRepository.findById(UUID.fromString(id))
            .orElseThrow { NoSuchElementException("Client not found") }
        return ResponseEntity.ok(UserDTO(
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
        ))
    }

    @GetMapping("/clients/{id}/reservations")
    fun getClientReservations(@PathVariable id: String): ResponseEntity<List<ReservationDTO>> {
        val reservations = reservationService.getUserReservations(id)
        return ResponseEntity.ok(reservations)
    }

    @GetMapping("/clients/{id}/notes")
    fun getClientNotes(@PathVariable id: String): ResponseEntity<List<Map<String, Any>>> {
        // Notes functionality - return empty for now
        return ResponseEntity.ok(emptyList())
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

    // ============ TRAINING PLANS ============

    @GetMapping("/plans")
    fun getPlans(): ResponseEntity<List<AdminTrainingPlanDTO>> {
        val plans = trainingPlanRepository.findAll().map { it.toAdminDTO() }
        return ResponseEntity.ok(plans)
    }

    @GetMapping("/plans/{id}")
    fun getPlan(@PathVariable id: String): ResponseEntity<AdminTrainingPlanDTO> {
        val plan = trainingPlanRepository.findById(UUID.fromString(id))
            .orElseThrow { NoSuchElementException("Plan not found") }
        return ResponseEntity.ok(plan.toAdminDTO())
    }

    @PostMapping("/plans")
    fun createPlan(@RequestBody request: CreateTrainingPlanRequest): ResponseEntity<AdminTrainingPlanDTO> {
        val plan = TrainingPlan(
            name = request.nameCs,
            nameCs = request.nameCs,
            nameEn = request.nameEn,
            description = request.descriptionCs,
            descriptionCs = request.descriptionCs,
            descriptionEn = request.descriptionEn,
            credits = request.credits,
            price = java.math.BigDecimal.ZERO,
            isActive = request.isActive
        )
        val saved = trainingPlanRepository.save(plan)
        return ResponseEntity.status(HttpStatus.CREATED).body(saved.toAdminDTO())
    }

    @RequestMapping(value = ["/plans/{id}"], method = [RequestMethod.PUT, RequestMethod.PATCH])
    fun updatePlan(
        @PathVariable id: String,
        @RequestBody request: UpdateTrainingPlanRequest
    ): ResponseEntity<AdminTrainingPlanDTO> {
        val existing = trainingPlanRepository.findById(UUID.fromString(id))
            .orElseThrow { NoSuchElementException("Plan not found") }

        val updated = existing.copy(
            name = request.nameCs ?: existing.name,
            nameCs = request.nameCs ?: existing.nameCs,
            nameEn = request.nameEn ?: existing.nameEn,
            description = request.descriptionCs ?: existing.description,
            descriptionCs = request.descriptionCs ?: existing.descriptionCs,
            descriptionEn = request.descriptionEn ?: existing.descriptionEn,
            credits = request.credits ?: existing.credits,
            isActive = request.isActive ?: existing.isActive
        )

        val saved = trainingPlanRepository.save(updated)
        return ResponseEntity.ok(saved.toAdminDTO())
    }

    @DeleteMapping("/plans/{id}")
    fun deletePlan(@PathVariable id: String): ResponseEntity<Map<String, String>> {
        val uuid = UUID.fromString(id)
        if (!trainingPlanRepository.existsById(uuid)) {
            throw NoSuchElementException("Plan not found")
        }
        trainingPlanRepository.deleteById(uuid)
        return ResponseEntity.ok(mapOf("message" to "Plan deleted"))
    }

    private fun TrainingPlan.toAdminDTO() = AdminTrainingPlanDTO(
        id = id.toString(),
        nameCs = nameCs,
        nameEn = nameEn,
        descriptionCs = descriptionCs,
        descriptionEn = descriptionEn,
        credits = credits,
        isActive = isActive,
        createdAt = createdAt.toString()
    )

    // ============ AVAILABILITY BLOCKS ============

    @GetMapping("/blocks")
    fun getAllBlocks(): ResponseEntity<List<AvailabilityBlockDTO>> {
        val blocks = availabilityBlockRepository.findAll().map { it.toDTO() }
        return ResponseEntity.ok(blocks)
    }

    @GetMapping("/blocks/{id}")
    fun getBlock(@PathVariable id: String): ResponseEntity<AvailabilityBlockDTO> {
        val block = availabilityBlockRepository.findById(UUID.fromString(id))
            .orElseThrow { NoSuchElementException("Block not found") }
        return ResponseEntity.ok(block.toDTO())
    }

    @PostMapping("/blocks")
    fun createBlock(@RequestBody request: CreateAvailabilityBlockRequest): ResponseEntity<Any> {
        val startTime = LocalTime.parse(request.startTime)
        val endTime = LocalTime.parse(request.endTime)

        // Check for overlapping blocks (only for non-blocked availability blocks)
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
            isActive = true
        )
        val saved = availabilityBlockRepository.save(block)
        return ResponseEntity.status(HttpStatus.CREATED).body(saved.toDTO())
    }

    @RequestMapping(value = ["/blocks/{id}"], method = [RequestMethod.PUT, RequestMethod.PATCH])
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

        // Check for overlapping blocks (only for non-blocked availability blocks)
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
    fun deleteBlock(@PathVariable id: String): ResponseEntity<Map<String, String>> {
        val uuid = UUID.fromString(id)
        if (!availabilityBlockRepository.existsById(uuid)) {
            throw NoSuchElementException("Block not found")
        }
        availabilityBlockRepository.deleteById(uuid)
        return ResponseEntity.ok(mapOf("message" to "Block deleted"))
    }

    // ============ Calendar Slots Endpoints ============

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

    @PostMapping("/calendar/slots/block")
    fun blockSlot(@RequestBody request: BlockSlotRequest): ResponseEntity<Any> {
        return try {
            val date = LocalDate.parse(request.date)
            val startTime = LocalTime.parse(request.startTime)
            val endTime = LocalTime.parse(request.endTime)
            availabilityService.blockSlot(date, startTime, endTime, request.isBlocked)
            ResponseEntity.ok(mapOf("message" to if (request.isBlocked) "Slot blocked" else "Slot unblocked"))
        } catch (e: NoSuchElementException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("error" to e.message))
        }
    }

    // ============ Admin Reservation Management ============

    @PostMapping("/reservations")
    fun adminCreateReservation(@RequestBody request: AdminCreateReservationRequest): ResponseEntity<Any> {
        return try {
            val reservation = reservationService.adminCreateReservation(request)
            ResponseEntity.status(HttpStatus.CREATED).body(reservation)
        } catch (e: NoSuchElementException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("error" to e.message))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(mapOf("error" to e.message))
        }
    }

    @DeleteMapping("/reservations/{id}")
    fun adminCancelReservation(
        @PathVariable id: String,
        @RequestParam(defaultValue = "true") refundCredits: Boolean
    ): ResponseEntity<Any> {
        return try {
            val reservation = reservationService.adminCancelReservation(id, refundCredits)
            ResponseEntity.ok(reservation)
        } catch (e: NoSuchElementException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("error" to e.message))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(mapOf("error" to e.message))
        }
    }

    @PatchMapping("/reservations/{id}/note")
    fun updateReservationNote(
        @PathVariable id: String,
        @RequestBody request: UpdateReservationNoteRequest
    ): ResponseEntity<Any> {
        return try {
            val reservation = reservationService.updateReservationNote(id, request.note)
            ResponseEntity.ok(reservation)
        } catch (e: NoSuchElementException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("error" to e.message))
        }
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
