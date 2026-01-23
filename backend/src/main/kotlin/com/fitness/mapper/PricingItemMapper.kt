package com.fitness.mapper

import com.fitness.dto.AdminPricingItemDTO
import com.fitness.dto.PricingItemDTO
import com.fitness.entity.PricingItem
import org.springframework.stereotype.Component

@Component
class PricingItemMapper {
    /**
     * Convert PricingItem entity to public PricingItemDTO.
     */
    fun toDTO(item: PricingItem): PricingItemDTO {
        return PricingItemDTO(
            id = item.id.toString(),
            nameCs = item.nameCs,
            nameEn = item.nameEn,
            descriptionCs = item.descriptionCs,
            descriptionEn = item.descriptionEn,
            credits = item.credits,
            isActive = item.isActive,
            sortOrder = item.sortOrder
        )
    }

    /**
     * Convert PricingItem entity to AdminPricingItemDTO.
     */
    fun toAdminDTO(item: PricingItem): AdminPricingItemDTO {
        return AdminPricingItemDTO(
            id = item.id.toString(),
            nameCs = item.nameCs,
            nameEn = item.nameEn,
            descriptionCs = item.descriptionCs,
            descriptionEn = item.descriptionEn,
            credits = item.credits,
            durationMinutes = item.durationMinutes,
            isActive = item.isActive,
            sortOrder = item.sortOrder,
            createdAt = item.createdAt.toString()
        )
    }

    /**
     * Batch convert PricingItem entities to AdminPricingItemDTO.
     */
    fun toAdminDTOBatch(items: List<PricingItem>): List<AdminPricingItemDTO> {
        return items.map { toAdminDTO(it) }
    }

    /**
     * Batch convert PricingItem entities to public PricingItemDTO.
     */
    fun toDTOBatch(items: List<PricingItem>): List<PricingItemDTO> {
        return items.map { toDTO(it) }
    }
}
