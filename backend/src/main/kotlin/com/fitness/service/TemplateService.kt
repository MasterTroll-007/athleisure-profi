package com.fitness.service

import com.fitness.dto.*
import com.fitness.entity.SlotTemplate
import com.fitness.entity.TemplatePricingItem
import com.fitness.entity.TemplateSlot
import com.fitness.entity.TrainingLocation
import com.fitness.repository.SlotTemplateRepository
import com.fitness.repository.TemplatePricingItemRepository
import com.fitness.repository.TemplateSlotRepository
import com.fitness.repository.TrainingLocationRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.*

@Service
class TemplateService(
    private val templateRepository: SlotTemplateRepository,
    private val templateSlotRepository: TemplateSlotRepository,
    private val templatePricingItemRepository: TemplatePricingItemRepository,
    private val locationRepository: TrainingLocationRepository
) {

    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    fun getAllTemplates(): List<SlotTemplateDTO> {
        val templates = templateRepository.findAll()
        return templates.map { buildTemplateDTO(it) }
    }

    fun getActiveTemplates(): List<SlotTemplateDTO> {
        val templates = templateRepository.findByIsActiveTrue()
        return templates.map { buildTemplateDTO(it) }
    }

    fun getTemplate(id: UUID): SlotTemplateDTO {
        val template = templateRepository.findById(id)
            .orElseThrow { IllegalArgumentException("Template not found") }
        return buildTemplateDTO(template)
    }

    @Transactional
    fun createTemplate(request: CreateTemplateRequest): SlotTemplateDTO {
        val templateLocationId = request.locationId?.let { UUID.fromString(it) }
        val template = SlotTemplate(name = request.name, locationId = templateLocationId)
        val savedTemplate = templateRepository.save(template)

        persistSlotsWithChildren(savedTemplate.id!!, request.slots)
        return buildTemplateDTO(savedTemplate)
    }

    @Transactional
    fun updateTemplate(id: UUID, request: UpdateTemplateRequest): SlotTemplateDTO {
        val template = templateRepository.findById(id)
            .orElseThrow { IllegalArgumentException("Template not found") }

        request.name?.let { template.name = it }
        request.isActive?.let { template.isActive = it }
        when {
            request.clearLocation == true -> template.locationId = null
            request.locationId != null -> template.locationId = UUID.fromString(request.locationId)
        }

        val savedTemplate = templateRepository.save(template)

        if (request.slots != null) {
            // Replace existing slots (and their pricing-item join rows)
            val existingSlots = templateSlotRepository.findByTemplateId(id)
            existingSlots.forEach { slot ->
                slot.id?.let { templatePricingItemRepository.deleteByTemplateSlotId(it) }
            }
            templateSlotRepository.deleteByTemplateId(id)
            persistSlotsWithChildren(savedTemplate.id!!, request.slots)
        }

        return buildTemplateDTO(savedTemplate)
    }

    @Transactional
    fun deleteTemplate(id: UUID) {
        if (!templateRepository.existsById(id)) {
            throw IllegalArgumentException("Template not found")
        }
        val existingSlots = templateSlotRepository.findByTemplateId(id)
        existingSlots.forEach { slot ->
            slot.id?.let { templatePricingItemRepository.deleteByTemplateSlotId(it) }
        }
        templateSlotRepository.deleteByTemplateId(id)
        templateRepository.deleteById(id)
    }

    private fun persistSlotsWithChildren(templateId: UUID, slotDtos: List<TemplateSlotDTO>) {
        val slots = slotDtos.map { slotDto ->
            TemplateSlot(
                templateId = templateId,
                dayOfWeek = DayOfWeek.of(slotDto.dayOfWeek),
                startTime = LocalTime.parse(slotDto.startTime, timeFormatter),
                endTime = LocalTime.parse(slotDto.endTime, timeFormatter),
                durationMinutes = slotDto.durationMinutes,
                locationId = slotDto.locationId?.let { UUID.fromString(it) }
            )
        }
        val savedSlots = templateSlotRepository.saveAll(slots)
        // Persist pricing-item joins in dto order (savedSlots preserves saveAll order).
        savedSlots.forEachIndexed { idx, savedSlot ->
            val pricingIds = slotDtos[idx].pricingItemIds
            pricingIds.forEach { pid ->
                templatePricingItemRepository.save(
                    TemplatePricingItem(
                        templateSlotId = savedSlot.id!!,
                        pricingItemId = UUID.fromString(pid)
                    )
                )
            }
        }
    }

    private fun buildTemplateDTO(template: SlotTemplate): SlotTemplateDTO {
        val slots = templateSlotRepository.findByTemplateId(template.id!!)
        val slotIds = slots.mapNotNull { it.id }
        val pricingJoins = if (slotIds.isNotEmpty()) {
            templatePricingItemRepository.findByTemplateSlotIdIn(slotIds)
        } else emptyList()
        val pricingBySlot = pricingJoins.groupBy({ it.templateSlotId }) { it.pricingItemId.toString() }

        // Resolve all locations referenced (template + per-slot) in one query.
        val locationIds = (slots.mapNotNull { it.locationId } + listOfNotNull(template.locationId)).toSet()
        val locationMap = if (locationIds.isNotEmpty()) {
            locationRepository.findAllById(locationIds).associateBy { it.id!! }
        } else emptyMap()

        val templateLocation = template.locationId?.let { locationMap[it] }

        return SlotTemplateDTO(
            id = template.id.toString(),
            name = template.name,
            slots = slots.map { slot ->
                val slotLocation = slot.locationId?.let { locationMap[it] }
                TemplateSlotDTO(
                    id = slot.id.toString(),
                    dayOfWeek = slot.dayOfWeek.value,
                    startTime = slot.startTime.format(timeFormatter),
                    endTime = slot.endTime.format(timeFormatter),
                    durationMinutes = slot.durationMinutes,
                    pricingItemIds = pricingBySlot[slot.id] ?: emptyList(),
                    locationId = slot.locationId?.toString(),
                    locationName = slotLocation?.nameCs,
                    locationColor = slotLocation?.color
                )
            }.sortedWith(compareBy({ it.dayOfWeek }, { it.startTime })),
            isActive = template.isActive,
            locationId = template.locationId?.toString(),
            locationName = templateLocation?.nameCs,
            locationColor = templateLocation?.color,
            createdAt = template.createdAt.toString()
        )
    }
}
