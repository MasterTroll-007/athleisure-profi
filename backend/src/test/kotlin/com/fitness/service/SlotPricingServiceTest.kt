package com.fitness.service

import com.fitness.entity.PricingItem
import com.fitness.entity.SlotPricingItem
import com.fitness.repository.PricingItemRepository
import com.fitness.repository.SlotPricingItemRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.UUID

class SlotPricingServiceTest {
    private val slotPricingItemRepository = mockk<SlotPricingItemRepository>(relaxed = true)
    private val pricingItemRepository = mockk<PricingItemRepository>()
    private val service = SlotPricingService(slotPricingItemRepository, pricingItemRepository)

    @Test
    fun `loading pricing items returns empty map without unnecessary repository work`() {
        assertThat(service.loadPricingItemsForSlots(emptyList())).isEmpty()

        val slotId = UUID.randomUUID()
        every { slotPricingItemRepository.findBySlotIdIn(listOf(slotId)) } returns emptyList()

        assertThat(service.loadPricingItemsForSlots(listOf(slotId))).isEmpty()
        verify(exactly = 0) { pricingItemRepository.findAllById(any<List<UUID>>()) }
    }

    @Test
    fun `loading pricing items groups summaries by slot and drops stale links`() {
        val slotA = UUID.randomUUID()
        val slotB = UUID.randomUUID()
        val priceA = UUID.randomUUID()
        val priceB = UUID.randomUUID()
        val stalePrice = UUID.randomUUID()
        every { slotPricingItemRepository.findBySlotIdIn(listOf(slotA, slotB)) } returns listOf(
            SlotPricingItem(slotId = slotA, pricingItemId = priceA),
            SlotPricingItem(slotId = slotA, pricingItemId = stalePrice),
            SlotPricingItem(slotId = slotB, pricingItemId = priceB)
        )
        every { pricingItemRepository.findAllById(listOf(priceA, stalePrice, priceB)) } returns listOf(
            PricingItem(id = priceA, nameCs = "Solo", nameEn = "Solo EN", credits = 2),
            PricingItem(id = priceB, nameCs = "Duo", nameEn = null, credits = 3)
        )

        val result = service.loadPricingItemsForSlots(listOf(slotA, slotB))

        assertThat(result[slotA]).hasSize(1)
        assertThat(result[slotA]!!.single().nameCs).isEqualTo("Solo")
        assertThat(result[slotA]!!.single().credits).isEqualTo(2)
        assertThat(result[slotB]).hasSize(1)
        assertThat(result[slotB]!!.single().nameCs).isEqualTo("Duo")
    }

    @Test
    fun `saving pricing items stores distinct ids only`() {
        val slotId = UUID.randomUUID()
        val priceA = UUID.randomUUID()
        val priceB = UUID.randomUUID()
        every { slotPricingItemRepository.save(any()) } answers { firstArg() }

        service.savePricingItemsForSlot(slotId, listOf(priceA.toString(), priceA.toString(), priceB.toString()))

        verify(exactly = 1) {
            slotPricingItemRepository.save(match { it.slotId == slotId && it.pricingItemId == priceA })
        }
        verify(exactly = 1) {
            slotPricingItemRepository.save(match { it.slotId == slotId && it.pricingItemId == priceB })
        }
    }
}
