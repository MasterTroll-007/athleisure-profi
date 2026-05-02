package com.fitness.service

import com.fitness.dto.PricingItemSummary
import com.fitness.entity.SlotPricingItem
import com.fitness.repository.PricingItemRepository
import com.fitness.repository.SlotPricingItemRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class SlotPricingService(
    private val slotPricingItemRepository: SlotPricingItemRepository,
    private val pricingItemRepository: PricingItemRepository
) {
    fun loadPricingItemsForSlots(slotIds: List<UUID>): Map<UUID, List<PricingItemSummary>> {
        if (slotIds.isEmpty()) return emptyMap()
        val slotPricingItems = slotPricingItemRepository.findBySlotIdIn(slotIds)
        if (slotPricingItems.isEmpty()) return emptyMap()
        val pricingItemIds = slotPricingItems.map { it.pricingItemId }.distinct()
        val pricingItems = pricingItemRepository.findAllById(pricingItemIds).associateBy { it.id }
        return slotPricingItems.groupBy({ it.slotId }) { spi ->
            pricingItems[spi.pricingItemId]?.let {
                PricingItemSummary(it.id.toString(), it.nameCs, it.nameEn, it.credits)
            }
        }.mapValues { (_, value) -> value.filterNotNull() }
    }

    fun savePricingItemsForSlot(slotId: UUID, pricingItemIds: List<String>) {
        for (pricingItemId in pricingItemIds.distinct()) {
            slotPricingItemRepository.save(
                SlotPricingItem(slotId = slotId, pricingItemId = UUID.fromString(pricingItemId))
            )
        }
    }
}
