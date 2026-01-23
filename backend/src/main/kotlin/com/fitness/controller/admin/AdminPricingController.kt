package com.fitness.controller.admin

import com.fitness.dto.*
import com.fitness.entity.PricingItem
import com.fitness.mapper.PricingItemMapper
import com.fitness.repository.PricingItemRepository
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
@RequestMapping("/api/admin/pricing")
@PreAuthorize("hasRole('ADMIN')")
class AdminPricingController(
    private val pricingItemRepository: PricingItemRepository,
    private val pricingItemMapper: PricingItemMapper
) {
    @GetMapping
    fun getPricingItems(@AuthenticationPrincipal principal: UserPrincipal): ResponseEntity<List<AdminPricingItemDTO>> {
        val trainerId = UUID.fromString(principal.userId)
        val items = pricingItemRepository.findByAdminIdOrderBySortOrder(trainerId)
        return ResponseEntity.ok(pricingItemMapper.toAdminDTOBatch(items))
    }

    @GetMapping("/{id}")
    fun getPricingItem(@PathVariable id: String): ResponseEntity<AdminPricingItemDTO> {
        val item = pricingItemRepository.findById(UUID.fromString(id))
            .orElseThrow { NoSuchElementException("Pricing item not found") }
        return ResponseEntity.ok(pricingItemMapper.toAdminDTO(item))
    }

    @PostMapping
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
        return ResponseEntity.status(HttpStatus.CREATED).body(pricingItemMapper.toAdminDTO(saved))
    }

    @RequestMapping(value = ["/{id}"], method = [RequestMethod.PUT, RequestMethod.PATCH])
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
        return ResponseEntity.ok(pricingItemMapper.toAdminDTO(saved))
    }

    @DeleteMapping("/{id}")
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
}
