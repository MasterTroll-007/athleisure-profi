package com.fitness.controller

import com.fitness.dto.*
import com.fitness.entity.AvailabilityBlock
import com.fitness.entity.ClientNote
import com.fitness.entity.PackageHighlight
import com.fitness.entity.PricingItem
import com.fitness.entity.TrainingPlan
import com.fitness.repository.AvailabilityBlockRepository
import com.fitness.repository.ClientNoteRepository
import com.fitness.repository.CreditPackageRepository
import com.fitness.repository.PricingItemRepository
import com.fitness.repository.ReservationRepository
import com.fitness.repository.StripePaymentRepository
import com.fitness.repository.TrainingPlanRepository
import com.fitness.repository.UserRepository
import com.fitness.security.UserPrincipal
import com.fitness.service.AvailabilityBlockValidationService
import com.fitness.service.AvailabilityService
import com.fitness.service.CreditService
import com.fitness.service.FileStorageService
import com.fitness.service.ReservationService
import com.fitness.service.SlotService
import com.fitness.service.TemplateService
import jakarta.validation.Valid
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
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
    private val reservationRepository: ReservationRepository,
    private val creditService: CreditService,
    private val availabilityBlockRepository: AvailabilityBlockRepository,
    private val trainingPlanRepository: TrainingPlanRepository,
    private val availabilityService: AvailabilityService,
    private val slotService: SlotService,
    private val templateService: TemplateService,
    private val stripePaymentRepository: StripePaymentRepository,
    private val creditPackageRepository: CreditPackageRepository,
    private val pricingItemRepository: PricingItemRepository,
    private val clientNoteRepository: ClientNoteRepository,
    private val fileStorageService: FileStorageService,
    private val blockValidationService: AvailabilityBlockValidationService
) {

    @GetMapping("/clients")
    fun getClients(
        @AuthenticationPrincipal principal: UserPrincipal,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<PageDTO<UserDTO>> {
        val adminId = UUID.fromString(principal.userId)
        val pageable = PageRequest.of(page, size, Sort.by("createdAt").descending())
        // Only show clients assigned to this admin (trainer)
        val clientsPage = userRepository.findByTrainerId(adminId, pageable)

        val admin = userRepository.findById(adminId).orElse(null)
        val trainerName = admin?.let {
            listOfNotNull(it.firstName, it.lastName).joinToString(" ").ifEmpty { it.email }
        }

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
                    trainerId = user.trainerId?.toString(),
                    trainerName = trainerName,
                    calendarStartHour = user.calendarStartHour,
                    calendarEndHour = user.calendarEndHour,
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
    fun searchClients(
        @AuthenticationPrincipal principal: UserPrincipal,
        @RequestParam q: String
    ): ResponseEntity<List<UserDTO>> {
        val adminId = UUID.fromString(principal.userId)
        val admin = userRepository.findById(adminId).orElse(null)
        val trainerName = admin?.let {
            listOfNotNull(it.firstName, it.lastName).joinToString(" ").ifEmpty { it.email }
        }

        val clients = userRepository.searchClientsByTrainer(q, adminId).map { user ->
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
                trainerId = user.trainerId?.toString(),
                trainerName = trainerName,
                calendarStartHour = user.calendarStartHour,
                calendarEndHour = user.calendarEndHour,
                createdAt = user.createdAt.toString()
            )
        }
        return ResponseEntity.ok(clients)
    }

    @GetMapping("/clients/{id}")
    fun getClient(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable id: String
    ): ResponseEntity<UserDTO> {
        val adminId = UUID.fromString(principal.userId)
        val user = userRepository.findById(UUID.fromString(id))
            .orElseThrow { NoSuchElementException("Client not found") }

        // Verify client belongs to this admin
        if (user.trainerId != adminId) {
            throw AccessDeniedException("Access denied")
        }

        val admin = userRepository.findById(adminId).orElse(null)
        val trainerName = admin?.let {
            listOfNotNull(it.firstName, it.lastName).joinToString(" ").ifEmpty { it.email }
        }

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
            trainerId = user.trainerId?.toString(),
            trainerName = trainerName,
            calendarStartHour = user.calendarStartHour,
            calendarEndHour = user.calendarEndHour,
            createdAt = user.createdAt.toString()
        ))
    }

    @GetMapping("/clients/{id}/reservations")
    fun getClientReservations(@PathVariable id: String): ResponseEntity<List<ReservationDTO>> {
        val reservations = reservationService.getUserReservations(id)
        return ResponseEntity.ok(reservations)
    }

    @GetMapping("/clients/{id}/notes")
    fun getClientNotes(@PathVariable id: String): ResponseEntity<List<ClientNoteDTO>> {
        val clientId = UUID.fromString(id)
        val notes = clientNoteRepository.findByClientIdOrderByCreatedAtDesc(clientId).map { note ->
            val admin = userRepository.findById(note.adminId).orElse(null)
            ClientNoteDTO(
                id = note.id.toString(),
                clientId = note.clientId.toString(),
                adminId = note.adminId.toString(),
                adminName = admin?.let { "${it.firstName} ${it.lastName}" },
                content = note.content,
                createdAt = note.createdAt.toString(),
                updatedAt = note.updatedAt.toString()
            )
        }
        return ResponseEntity.ok(notes)
    }

    @PostMapping("/clients/{id}/notes")
    fun createClientNote(
        @PathVariable id: String,
        @AuthenticationPrincipal principal: UserPrincipal,
        @Valid @RequestBody request: CreateClientNoteRequest
    ): ResponseEntity<ClientNoteDTO> {
        val clientId = UUID.fromString(id)
        val adminId = UUID.fromString(principal.userId)
        val note = ClientNote(
            clientId = clientId,
            adminId = adminId,
            content = request.content
        )
        val saved = clientNoteRepository.save(note)
        val admin = userRepository.findById(adminId).orElse(null)
        return ResponseEntity.status(HttpStatus.CREATED).body(
            ClientNoteDTO(
                id = saved.id.toString(),
                clientId = saved.clientId.toString(),
                adminId = saved.adminId.toString(),
                adminName = admin?.let { "${it.firstName} ${it.lastName}" },
                content = saved.content,
                createdAt = saved.createdAt.toString(),
                updatedAt = saved.updatedAt.toString()
            )
        )
    }

    @DeleteMapping("/notes/{id}")
    fun deleteClientNote(@PathVariable id: String): ResponseEntity<Map<String, String>> {
        val noteId = UUID.fromString(id)
        if (!clientNoteRepository.existsById(noteId)) {
            throw NoSuchElementException("Note not found")
        }
        clientNoteRepository.deleteById(noteId)
        return ResponseEntity.ok(mapOf("message" to "Note deleted"))
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
        @Valid @RequestBody request: AdminAdjustCreditsRequest
    ): ResponseEntity<CreditBalanceResponse> {
        val balance = creditService.adjustCredits(principal.userId, request)
        return ResponseEntity.ok(balance)
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

    // ============ PAYMENTS ============

    @GetMapping("/payments")
    fun getPayments(@RequestParam(defaultValue = "100") limit: Int): ResponseEntity<List<AdminPaymentDTO>> {
        val payments = stripePaymentRepository.findAllByOrderByCreatedAtDesc()
            .take(limit)
            .map { payment ->
                val user = payment.userId?.let { userRepository.findById(it).orElse(null) }
                val creditPackage = payment.creditPackageId?.let { creditPackageRepository.findById(it).orElse(null) }

                AdminPaymentDTO(
                    id = payment.id.toString(),
                    userId = payment.userId?.toString(),
                    userName = user?.let { "${it.firstName} ${it.lastName}" },
                    gopayId = null,  // Not used for Stripe
                    stripeSessionId = payment.stripeSessionId,
                    amount = payment.amount,
                    currency = payment.currency,
                    state = mapStripeStatusToLegacy(payment.status),
                    creditPackageId = payment.creditPackageId?.toString(),
                    creditPackageName = creditPackage?.nameCs,
                    createdAt = payment.createdAt.toString(),
                    updatedAt = payment.updatedAt.toString()
                )
            }
        return ResponseEntity.ok(payments)
    }

    private fun mapStripeStatusToLegacy(stripeStatus: String): String {
        // Map Stripe statuses to GoPay-style statuses for frontend compatibility
        return when (stripeStatus.lowercase()) {
            "completed" -> "PAID"
            "pending" -> "CREATED"
            "expired" -> "TIMEOUTED"
            "refunded" -> "REFUNDED"
            else -> stripeStatus.uppercase()
        }
    }

    // ============ CREDIT PACKAGES ============

    @GetMapping("/packages")
    fun getPackages(@AuthenticationPrincipal principal: UserPrincipal): ResponseEntity<List<AdminCreditPackageDTO>> {
        val trainerId = UUID.fromString(principal.userId)
        val packages = creditPackageRepository.findByTrainerIdOrderBySortOrder(trainerId)
        return ResponseEntity.ok(packages.map { it.toAdminDTO(trainerId) })
    }

    @GetMapping("/packages/{id}")
    fun getPackage(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable id: String
    ): ResponseEntity<AdminCreditPackageDTO> {
        val trainerId = UUID.fromString(principal.userId)
        val pkg = creditPackageRepository.findById(UUID.fromString(id))
            .orElseThrow { NoSuchElementException("Package not found") }
        return ResponseEntity.ok(pkg.toAdminDTO(trainerId))
    }

    @PostMapping("/packages")
    fun createPackage(
        @AuthenticationPrincipal principal: UserPrincipal,
        @Valid @RequestBody request: CreateCreditPackageRequest
    ): ResponseEntity<AdminCreditPackageDTO> {
        val trainerId = UUID.fromString(principal.userId)

        // If this package is marked as basic, unset basic on other packages
        if (request.isBasic) {
            val existingPackages = creditPackageRepository.findByTrainerIdOrderBySortOrder(trainerId)
            existingPackages.filter { it.isBasic }.forEach {
                creditPackageRepository.save(it.copy(isBasic = false))
            }
        }

        val pkg = com.fitness.entity.CreditPackage(
            trainerId = trainerId,
            nameCs = request.nameCs,
            nameEn = request.nameEn,
            description = request.description,
            credits = request.credits,
            priceCzk = request.priceCzk,
            currency = request.currency,
            isActive = request.isActive,
            sortOrder = request.sortOrder,
            highlightType = PackageHighlight.valueOf(request.highlightType),
            isBasic = request.isBasic
        )
        val saved = creditPackageRepository.save(pkg)
        return ResponseEntity.status(HttpStatus.CREATED).body(saved.toAdminDTO(trainerId))
    }

    @RequestMapping(value = ["/packages/{id}"], method = [RequestMethod.PUT, RequestMethod.PATCH])
    fun updatePackage(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable id: String,
        @Valid @RequestBody request: UpdateCreditPackageRequest
    ): ResponseEntity<AdminCreditPackageDTO> {
        val trainerId = UUID.fromString(principal.userId)
        val existing = creditPackageRepository.findById(UUID.fromString(id))
            .orElseThrow { NoSuchElementException("Package not found") }

        // Verify package belongs to this trainer
        if (existing.trainerId != trainerId) {
            throw AccessDeniedException("Access denied")
        }

        // If this package is being marked as basic, unset basic on other packages
        if (request.isBasic == true && !existing.isBasic) {
            val existingPackages = creditPackageRepository.findByTrainerIdOrderBySortOrder(trainerId)
            existingPackages.filter { it.isBasic && it.id != existing.id }.forEach {
                creditPackageRepository.save(it.copy(isBasic = false))
            }
        }

        val updated = existing.copy(
            nameCs = request.nameCs ?: existing.nameCs,
            nameEn = request.nameEn ?: existing.nameEn,
            description = request.description ?: existing.description,
            credits = request.credits ?: existing.credits,
            priceCzk = request.priceCzk ?: existing.priceCzk,
            currency = request.currency ?: existing.currency,
            isActive = request.isActive ?: existing.isActive,
            sortOrder = request.sortOrder ?: existing.sortOrder,
            highlightType = request.highlightType?.let { PackageHighlight.valueOf(it) } ?: existing.highlightType,
            isBasic = request.isBasic ?: existing.isBasic
        )

        val saved = creditPackageRepository.save(updated)
        return ResponseEntity.ok(saved.toAdminDTO(trainerId))
    }

    @DeleteMapping("/packages/{id}")
    fun deletePackage(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable id: String
    ): ResponseEntity<Map<String, String>> {
        val trainerId = UUID.fromString(principal.userId)
        val uuid = UUID.fromString(id)
        val existing = creditPackageRepository.findById(uuid)
            .orElseThrow { NoSuchElementException("Package not found") }

        // Verify package belongs to this trainer
        if (existing.trainerId != trainerId) {
            throw AccessDeniedException("Access denied")
        }

        creditPackageRepository.deleteById(uuid)
        return ResponseEntity.ok(mapOf("message" to "Package deleted"))
    }

    private fun com.fitness.entity.CreditPackage.toAdminDTO(trainerId: UUID): AdminCreditPackageDTO {
        // Find basic package to calculate discount
        val basicPackage = creditPackageRepository.findByTrainerIdOrderBySortOrder(trainerId).find { it.isBasic }
        val basicPricePerCredit = basicPackage?.let {
            if (it.credits > 0) it.priceCzk.toDouble() / it.credits else null
        }

        val pricePerCredit = if (credits > 0) priceCzk.toDouble() / credits else 0.0
        val discountPercent = if (basicPricePerCredit != null && basicPricePerCredit > 0 && !isBasic) {
            ((basicPricePerCredit - pricePerCredit) / basicPricePerCredit * 100).toInt()
        } else null

        return AdminCreditPackageDTO(
            id = id.toString(),
            nameCs = nameCs,
            nameEn = nameEn,
            description = description,
            credits = credits,
            priceCzk = priceCzk,
            currency = currency,
            isActive = isActive,
            sortOrder = sortOrder,
            highlightType = highlightType.name,
            isBasic = isBasic,
            discountPercent = discountPercent,
            createdAt = createdAt.toString()
        )
    }

    // ============ PRICING ITEMS ============

    @GetMapping("/pricing")
    fun getPricingItems(@AuthenticationPrincipal principal: UserPrincipal): ResponseEntity<List<AdminPricingItemDTO>> {
        val trainerId = UUID.fromString(principal.userId)
        val items = pricingItemRepository.findByAdminIdOrderBySortOrder(trainerId).map { it.toAdminDTO() }
        return ResponseEntity.ok(items)
    }

    @GetMapping("/pricing/{id}")
    fun getPricingItem(@PathVariable id: String): ResponseEntity<AdminPricingItemDTO> {
        val item = pricingItemRepository.findById(UUID.fromString(id))
            .orElseThrow { NoSuchElementException("Pricing item not found") }
        return ResponseEntity.ok(item.toAdminDTO())
    }

    @PostMapping("/pricing")
    fun createPricingItem(
        @AuthenticationPrincipal principal: UserPrincipal,
        @Valid @RequestBody request: CreatePricingItemRequest
    ): ResponseEntity<AdminPricingItemDTO> {
        val trainerId = UUID.fromString(principal.userId)
        val item = PricingItem(
            adminId = trainerId,
            nameCs = request.nameCs,
            nameEn = request.nameEn,
            descriptionCs = request.descriptionCs,
            descriptionEn = request.descriptionEn,
            credits = request.credits,
            durationMinutes = request.durationMinutes,
            isActive = request.isActive,
            sortOrder = request.sortOrder
        )
        val saved = pricingItemRepository.save(item)
        return ResponseEntity.status(HttpStatus.CREATED).body(saved.toAdminDTO())
    }

    @RequestMapping(value = ["/pricing/{id}"], method = [RequestMethod.PUT, RequestMethod.PATCH])
    fun updatePricingItem(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable id: String,
        @Valid @RequestBody request: UpdatePricingItemRequest
    ): ResponseEntity<AdminPricingItemDTO> {
        val trainerId = UUID.fromString(principal.userId)
        val existing = pricingItemRepository.findById(UUID.fromString(id))
            .orElseThrow { NoSuchElementException("Pricing item not found") }

        // Verify item belongs to this trainer
        if (existing.adminId != trainerId) {
            throw AccessDeniedException("Access denied")
        }

        val updated = existing.copy(
            nameCs = request.nameCs ?: existing.nameCs,
            nameEn = request.nameEn ?: existing.nameEn,
            descriptionCs = request.descriptionCs ?: existing.descriptionCs,
            descriptionEn = request.descriptionEn ?: existing.descriptionEn,
            credits = request.credits ?: existing.credits,
            durationMinutes = request.durationMinutes ?: existing.durationMinutes,
            isActive = request.isActive ?: existing.isActive,
            sortOrder = request.sortOrder ?: existing.sortOrder
        )

        val saved = pricingItemRepository.save(updated)
        return ResponseEntity.ok(saved.toAdminDTO())
    }

    @DeleteMapping("/pricing/{id}")
    fun deletePricingItem(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable id: String
    ): ResponseEntity<Map<String, String>> {
        val trainerId = UUID.fromString(principal.userId)
        val uuid = UUID.fromString(id)
        val existing = pricingItemRepository.findById(uuid)
            .orElseThrow { NoSuchElementException("Pricing item not found") }

        // Verify item belongs to this trainer
        if (existing.adminId != trainerId) {
            throw AccessDeniedException("Access denied")
        }

        pricingItemRepository.deleteById(uuid)
        return ResponseEntity.ok(mapOf("message" to "Pricing item deleted"))
    }

    private fun PricingItem.toAdminDTO() = AdminPricingItemDTO(
        id = id.toString(),
        nameCs = nameCs,
        nameEn = nameEn,
        descriptionCs = descriptionCs,
        descriptionEn = descriptionEn,
        credits = credits,
        durationMinutes = durationMinutes,
        isActive = isActive,
        sortOrder = sortOrder,
        createdAt = createdAt.toString()
    )

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
    fun createPlan(@Valid @RequestBody request: CreateTrainingPlanRequest): ResponseEntity<AdminTrainingPlanDTO> {
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
        @Valid @RequestBody request: UpdateTrainingPlanRequest
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
        val plan = trainingPlanRepository.findById(uuid)
            .orElseThrow { NoSuchElementException("Plan not found") }

        // Delete associated file if exists
        fileStorageService.deletePlanFile(plan.filePath)

        trainingPlanRepository.deleteById(uuid)
        return ResponseEntity.ok(mapOf("message" to "Plan deleted"))
    }

    @PostMapping("/plans/{id}/upload", consumes = ["multipart/form-data"])
    fun uploadPlanFile(
        @PathVariable id: String,
        @RequestParam("file") file: MultipartFile
    ): ResponseEntity<AdminTrainingPlanDTO> {
        val uuid = UUID.fromString(id)
        val existing = trainingPlanRepository.findById(uuid)
            .orElseThrow { NoSuchElementException("Plan not found") }

        // Validate file type
        val contentType = file.contentType ?: ""
        if (!contentType.contains("pdf")) {
            return ResponseEntity.badRequest().build()
        }

        // Delete old file if exists
        fileStorageService.deletePlanFile(existing.filePath)

        // Store new file
        val filePath = fileStorageService.storePlanFile(file, uuid)

        // Update plan with file path
        val updated = existing.copy(filePath = filePath)
        val saved = trainingPlanRepository.save(updated)

        return ResponseEntity.ok(saved.toAdminDTO())
    }

    @DeleteMapping("/plans/{id}/file")
    fun deletePlanFile(@PathVariable id: String): ResponseEntity<AdminTrainingPlanDTO> {
        val uuid = UUID.fromString(id)
        val existing = trainingPlanRepository.findById(uuid)
            .orElseThrow { NoSuchElementException("Plan not found") }

        // Delete file
        fileStorageService.deletePlanFile(existing.filePath)

        // Update plan
        val updated = existing.copy(filePath = null)
        val saved = trainingPlanRepository.save(updated)

        return ResponseEntity.ok(saved.toAdminDTO())
    }

    private fun TrainingPlan.toAdminDTO() = AdminTrainingPlanDTO(
        id = id.toString(),
        nameCs = nameCs,
        nameEn = nameEn,
        descriptionCs = descriptionCs,
        descriptionEn = descriptionEn,
        credits = credits,
        isActive = isActive,
        filePath = filePath,
        previewImage = previewImage,
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
        return ResponseEntity.status(HttpStatus.CREATED).body(saved.toDTO())
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

    // ============ NEW SLOTS SYSTEM ============

    @GetMapping("/slots")
    fun getSlots(
        @RequestParam start: String,
        @RequestParam end: String
    ): ResponseEntity<List<SlotDTO>> {
        val startDate = LocalDate.parse(start)
        val endDate = LocalDate.parse(end)
        val slots = slotService.getSlots(startDate, endDate)
        return ResponseEntity.ok(slots)
    }

    @PostMapping("/slots")
    fun createSlot(@Valid @RequestBody request: CreateSlotRequest): ResponseEntity<Any> {
        return try {
            val slot = slotService.createSlot(request)
            ResponseEntity.status(HttpStatus.CREATED).body(slot)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(mapOf("error" to e.message))
        }
    }

    @PatchMapping("/slots/{id}")
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

    @DeleteMapping("/slots/{id}")
    fun deleteSlot(@PathVariable id: String): ResponseEntity<Any> {
        return try {
            slotService.deleteSlot(UUID.fromString(id))
            ResponseEntity.ok(mapOf("message" to "Slot deleted"))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("error" to e.message))
        }
    }

    @PostMapping("/slots/unlock-week")
    fun unlockWeek(@Valid @RequestBody request: UnlockWeekRequest): ResponseEntity<Map<String, Any>> {
        val weekStartDate = LocalDate.parse(request.weekStartDate)
        val count = slotService.unlockWeek(weekStartDate)
        return ResponseEntity.ok(mapOf("message" to "Week unlocked", "unlockedCount" to count))
    }

    @PostMapping("/slots/apply-template")
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

    // ============ TEMPLATES ============

    @GetMapping("/templates")
    fun getTemplates(): ResponseEntity<List<SlotTemplateDTO>> {
        val templates = templateService.getAllTemplates()
        return ResponseEntity.ok(templates)
    }

    @GetMapping("/templates/{id}")
    fun getTemplate(@PathVariable id: String): ResponseEntity<Any> {
        return try {
            val template = templateService.getTemplate(UUID.fromString(id))
            ResponseEntity.ok(template)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("error" to e.message))
        }
    }

    @PostMapping("/templates")
    fun createTemplate(@Valid @RequestBody request: CreateTemplateRequest): ResponseEntity<SlotTemplateDTO> {
        val template = templateService.createTemplate(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(template)
    }

    @PatchMapping("/templates/{id}")
    fun updateTemplate(
        @PathVariable id: String,
        @Valid @RequestBody request: UpdateTemplateRequest
    ): ResponseEntity<Any> {
        return try {
            val template = templateService.updateTemplate(UUID.fromString(id), request)
            ResponseEntity.ok(template)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(mapOf("error" to e.message))
        }
    }

    @DeleteMapping("/templates/{id}")
    fun deleteTemplate(@PathVariable id: String): ResponseEntity<Any> {
        return try {
            templateService.deleteTemplate(UUID.fromString(id))
            ResponseEntity.ok(mapOf("message" to "Template deleted"))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("error" to e.message))
        }
    }

    // ============ Admin Reservation Management ============

    @PostMapping("/reservations")
    fun adminCreateReservation(@Valid @RequestBody request: AdminCreateReservationRequest): ResponseEntity<Any> {
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
        @Valid @RequestBody request: UpdateReservationNoteRequest
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

    // ============ ADMIN SETTINGS ============

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

        // Generate new 8-character invite code
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

    @GetMapping("/trainers")
    fun getTrainers(): ResponseEntity<List<TrainerDTO>> {
        val trainers = userRepository.findByRole("admin").map { trainer ->
            TrainerDTO(
                id = trainer.id.toString(),
                email = trainer.email,
                firstName = trainer.firstName,
                lastName = trainer.lastName,
                calendarStartHour = trainer.calendarStartHour,
                calendarEndHour = trainer.calendarEndHour
            )
        }
        return ResponseEntity.ok(trainers)
    }

    @PostMapping("/clients/{id}/assign-trainer")
    fun assignTrainer(
        @PathVariable id: String,
        @Valid @RequestBody request: AssignTrainerRequest
    ): ResponseEntity<UserDTO> {
        val client = userRepository.findById(UUID.fromString(id))
            .orElseThrow { NoSuchElementException("Client not found") }

        val trainerId = if (request.trainerId.isBlank()) null else UUID.fromString(request.trainerId)

        // Verify trainer exists and is admin
        if (trainerId != null) {
            val trainer = userRepository.findById(trainerId)
                .orElseThrow { NoSuchElementException("Trainer not found") }
            if (trainer.role != "admin") {
                throw IllegalArgumentException("Selected user is not a trainer")
            }
        }

        val updated = client.copy(trainerId = trainerId)
        val saved = userRepository.save(updated)

        val trainerName = saved.trainerId?.let { tid ->
            userRepository.findById(tid).orElse(null)?.let { t ->
                listOfNotNull(t.firstName, t.lastName).joinToString(" ").ifEmpty { t.email }
            }
        }

        return ResponseEntity.ok(UserDTO(
            id = saved.id.toString(),
            email = saved.email,
            firstName = saved.firstName,
            lastName = saved.lastName,
            phone = saved.phone,
            role = saved.role,
            credits = saved.credits,
            locale = saved.locale,
            theme = saved.theme,
            trainerId = saved.trainerId?.toString(),
            trainerName = trainerName,
            calendarStartHour = saved.calendarStartHour,
            calendarEndHour = saved.calendarEndHour,
            createdAt = saved.createdAt.toString()
        ))
    }
}
