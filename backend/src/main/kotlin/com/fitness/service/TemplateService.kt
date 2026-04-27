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
import com.fitness.repository.PricingItemRepository
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
    private val pricingItemRepository: PricingItemRepository,
    private val locationRepository: TrainingLocationRepository
) {

    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    private fun requireTemplateOwnedByAdmin(template: SlotTemplate, adminId: UUID) {
        if (template.adminId != adminId) {
            throw org.springframework.security.access.AccessDeniedException("Access denied")
        }
    }

    private fun requireLocationOwnedByAdmin(locationId: UUID?, adminId: UUID) {
        if (locationId == null) return
        val location = locationRepository.findById(locationId)
            .orElseThrow { IllegalArgumentException("Training location not found") }
        if (location.adminId != adminId) {
            throw org.springframework.security.access.AccessDeniedException("Training location does not belong to this trainer")
        }
    }

    private fun requirePricingItemsOwnedByAdmin(pricingItemIds: List<String>, adminId: UUID) {
        if (pricingItemIds.isEmpty()) return
        val ids = pricingItemIds.map { UUID.fromString(it) }
        val items = pricingItemRepository.findAllById(ids)
        if (items.size != ids.size) {
            throw IllegalArgumentException("Pricing item not found")
        }
        if (items.any { it.adminId != adminId }) {
            throw org.springframework.security.access.AccessDeniedException("Pricing item does not belong to this trainer")
        }
    }

    fun getAllTemplates(adminId: UUID): List<SlotTemplateDTO> {
        val templates = templateRepository.findByAdminIdOrderByCreatedAtDesc(adminId)
        return templates.map { buildTemplateDTO(it) }
    }

    fun getActiveTemplates(): List<SlotTemplateDTO> {
        val templates = templateRepository.findByIsActiveTrue()
        return templates.map { buildTemplateDTO(it) }
    }

    fun getTemplate(id: UUID, adminId: UUID): SlotTemplateDTO {
        val template = templateRepository.findById(id)
            .orElseThrow { IllegalArgumentException("Template not found") }
        requireTemplateOwnedByAdmin(template, adminId)
        return buildTemplateDTO(template)
    }

    @Transactional
    fun createTemplate(request: CreateTemplateRequest, adminId: UUID): SlotTemplateDTO {
        val templateLocationId = request.locationId?.let { UUID.fromString(it) }
        requireLocationOwnedByAdmin(templateLocationId, adminId)
        request.slots.forEach { slot ->
            requireLocationOwnedByAdmin(slot.locationId?.let { UUID.fromString(it) }, adminId)
            requirePricingItemsOwnedByAdmin(slot.pricingItemIds, adminId)
        }
        val template = SlotTemplate(name = request.name, locationId = templateLocationId, adminId = adminId)
        val savedTemplate = templateRepository.save(template)

        persistSlotsWithChildren(savedTemplate.id!!, request.slots)
        return buildTemplateDTO(savedTemplate)
    }

    @Transactional
    fun updateTemplate(id: UUID, request: UpdateTemplateRequest, adminId: UUID): SlotTemplateDTO {
        val template = templateRepository.findById(id)
            .orElseThrow { IllegalArgumentException("Template not found") }
        requireTemplateOwnedByAdmin(template, adminId)

        request.name?.let { template.name = it }
        request.isActive?.let { template.isActive = it }
        when {
            request.clearLocation == true -> template.locationId = null
            request.locationId != null -> {
                val locationId = UUID.fromString(request.locationId)
                requireLocationOwnedByAdmin(locationId, adminId)
                template.locationId = locationId
            }
        }

        val savedTemplate = templateRepository.save(template)

        if (request.slots != null) {
            request.slots.forEach { slot ->
                requireLocationOwnedByAdmin(slot.locationId?.let { UUID.fromString(it) }, adminId)
                requirePricingItemsOwnedByAdmin(slot.pricingItemIds, adminId)
            }
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
    fun deleteTemplate(id: UUID, adminId: UUID) {
        val template = templateRepository.findById(id)
            .orElseThrow { IllegalArgumentException("Template not found") }
        requireTemplateOwnedByAdmin(template, adminId)
        val existingSlots = templateSlotRepository.findByTemplateId(id)
        existingSlots.forEach { slot ->
            slot.id?.let { templatePricingItemRepository.deleteByTemplateSlotId(it) }
        }
        templateSlotRepository.deleteByTemplateId(id)
        templateRepository.deleteById(id)
    }

    private fun persistSlotsWithChildren(templateId: UUID, slotDtos: List<TemplateSlotDTO>) {
        val slots = slotDtos.map { slotDto ->
            val startTime = LocalTime.parse(slotDto.startTime, timeFormatter)
            val endTime = LocalTime.parse(slotDto.endTime, timeFormatter)
            if (endTime <= startTime) {
                throw IllegalArgumentException("Template slot end time must be after start time")
            }
            TemplateSlot(
                templateId = templateId,
                dayOfWeek = DayOfWeek.of(slotDto.dayOfWeek),
                startTime = startTime,
                endTime = endTime,
                durationMinutes = slotDto.durationMinutes,
                capacity = slotDto.capacity,
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
                    capacity = slot.capacity,
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
