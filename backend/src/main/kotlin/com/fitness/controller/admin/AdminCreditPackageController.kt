package com.fitness.controller.admin

import com.fitness.dto.*
import com.fitness.entity.CreditPackage
import com.fitness.entity.PackageHighlight
import com.fitness.mapper.CreditPackageMapper
import com.fitness.mapper.PaymentMapper
import com.fitness.repository.CreditPackageRepository
import com.fitness.repository.StripePaymentRepository
import com.fitness.security.UserPrincipal
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
class AdminCreditPackageController(
    private val creditPackageRepository: CreditPackageRepository,
    private val stripePaymentRepository: StripePaymentRepository,
    private val creditPackageMapper: CreditPackageMapper,
    private val paymentMapper: PaymentMapper
) {
    @GetMapping("/packages")
    fun getPackages(@AuthenticationPrincipal principal: UserPrincipal): ResponseEntity<List<AdminCreditPackageDTO>> {
        val trainerId = UUID.fromString(principal.userId)
        val packages = creditPackageRepository.findByTrainerIdOrderBySortOrder(trainerId)
        return ResponseEntity.ok(creditPackageMapper.toAdminDTOBatch(packages, trainerId))
    }

    @GetMapping("/packages/{id}")
    fun getPackage(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable id: String
    ): ResponseEntity<AdminCreditPackageDTO> {
        val trainerId = UUID.fromString(principal.userId)
        val pkg = creditPackageRepository.findById(UUID.fromString(id))
            .orElseThrow { NoSuchElementException("Package not found") }
        return ResponseEntity.ok(creditPackageMapper.toAdminDTO(pkg, trainerId))
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

        val pkg = CreditPackage(
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
        return ResponseEntity.status(HttpStatus.CREATED).body(creditPackageMapper.toAdminDTO(saved, trainerId))
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
        return ResponseEntity.ok(creditPackageMapper.toAdminDTO(saved, trainerId))
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

    @GetMapping("/payments")
    fun getPayments(@RequestParam(defaultValue = "100") limit: Int): ResponseEntity<List<AdminPaymentDTO>> {
        val payments = stripePaymentRepository.findAllByOrderByCreatedAtDesc().take(limit)
        return ResponseEntity.ok(paymentMapper.toAdminDTOBatch(payments))
    }
}
