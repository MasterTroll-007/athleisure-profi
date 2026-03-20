package com.fitness.service

import com.fitness.entity.Slot
import com.fitness.entity.SlotPricingItem
import com.fitness.entity.SlotStatus
import com.fitness.repository.SlotPricingItemRepository
import com.fitness.repository.SlotRepository
import com.fitness.repository.SlotTemplateRepository
import com.fitness.repository.TemplatePricingItemRepository
import com.fitness.repository.TemplateSlotRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.*

@Service
class SlotAutoGeneratorService(
    private val slotRepository: SlotRepository,
    private val templateRepository: SlotTemplateRepository,
    private val templateSlotRepository: TemplateSlotRepository,
    private val slotPricingItemRepository: SlotPricingItemRepository,
    private val templatePricingItemRepository: TemplatePricingItemRepository
) {
    private val logger = LoggerFactory.getLogger(SlotAutoGeneratorService::class.java)

    /**
     * Auto-generate slots from active templates for the next week.
     * Runs every Sunday at 20:00.
     */
    @Scheduled(cron = "0 0 20 * * SUN")
    @Transactional
    fun autoGenerateSlotsForNextWeek() {
        logger.info("Starting auto-generation of slots for next week")

        val nextMonday = LocalDate.now().with(DayOfWeek.MONDAY).plusWeeks(1)
        val generatedCount = generateSlotsForWeek(nextMonday)

        logger.info("Auto-generated $generatedCount slots for week starting $nextMonday")
    }

    /**
     * Generate slots from all active templates for a given week.
     * Returns the number of slots created.
     */
    @Transactional
    fun generateSlotsForWeek(mondayDate: LocalDate): Int {
        val monday = if (mondayDate.dayOfWeek == DayOfWeek.MONDAY) {
            mondayDate
        } else {
            mondayDate.with(DayOfWeek.MONDAY)
        }

        val activeTemplates = templateRepository.findByIsActiveTrue()
        var createdCount = 0

        for (template in activeTemplates) {
            val templateSlots = templateSlotRepository.findByTemplateId(template.id)

            for (templateSlot in templateSlots) {
                val slotDate = monday.with(templateSlot.dayOfWeek)

                // Skip if slot already exists at this date/time
                if (slotRepository.existsOverlappingSlot(slotDate, templateSlot.startTime, templateSlot.endTime)) {
                    continue
                }

                val slot = slotRepository.save(
                    Slot(
                        date = slotDate,
                        startTime = templateSlot.startTime,
                        endTime = templateSlot.endTime,
                        durationMinutes = templateSlot.durationMinutes,
                        status = SlotStatus.LOCKED,
                        templateId = template.id
                    )
                )

                // Copy pricing items from template slot
                val templatePricingItems = templatePricingItemRepository.findByTemplateSlotId(templateSlot.id)
                for (tpi in templatePricingItems) {
                    slotPricingItemRepository.save(
                        SlotPricingItem(slotId = slot.id, pricingItemId = tpi.pricingItemId)
                    )
                }

                createdCount++
            }
        }

        return createdCount
    }
}
