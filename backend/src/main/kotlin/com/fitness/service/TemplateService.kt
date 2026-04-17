package com.fitness.service

import com.fitness.dto.*
import com.fitness.entity.SlotTemplate
import com.fitness.entity.TemplateSlot
import com.fitness.entity.TrainingLocation
import com.fitness.repository.SlotTemplateRepository
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
    private val locationRepository: TrainingLocationRepository
) {

    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    fun getAllTemplates(): List<SlotTemplateDTO> {
        val templates = templateRepository.findAll()
        val locationMap = buildLocationMap(templates)
        return templates.map { template ->
            val slots = templateSlotRepository.findByTemplateId(template.id!!)
            toDTO(template, slots, locationMap[template.locationId])
        }
    }

    fun getActiveTemplates(): List<SlotTemplateDTO> {
        val templates = templateRepository.findByIsActiveTrue()
        val locationMap = buildLocationMap(templates)
        return templates.map { template ->
            val slots = templateSlotRepository.findByTemplateId(template.id!!)
            toDTO(template, slots, locationMap[template.locationId])
        }
    }

    fun getTemplate(id: UUID): SlotTemplateDTO {
        val template = templateRepository.findById(id)
            .orElseThrow { IllegalArgumentException("Template not found") }
        val slots = templateSlotRepository.findByTemplateId(template.id!!)
        val location = template.locationId?.let { locationRepository.findById(it).orElse(null) }
        return toDTO(template, slots, location)
    }

    @Transactional
    fun createTemplate(request: CreateTemplateRequest): SlotTemplateDTO {
        val locationId = request.locationId?.let { UUID.fromString(it) }
        val template = SlotTemplate(name = request.name, locationId = locationId)
        val savedTemplate = templateRepository.save(template)

        val slots = request.slots.map { slotDto ->
            TemplateSlot(
                templateId = savedTemplate.id!!,
                dayOfWeek = DayOfWeek.of(slotDto.dayOfWeek),
                startTime = LocalTime.parse(slotDto.startTime, timeFormatter),
                endTime = LocalTime.parse(slotDto.endTime, timeFormatter),
                durationMinutes = slotDto.durationMinutes
            )
        }

        val savedSlots = templateSlotRepository.saveAll(slots)
        val location = locationId?.let { locationRepository.findById(it).orElse(null) }
        return toDTO(savedTemplate, savedSlots, location)
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

        // If slots are provided, replace all existing slots
        val slots = if (request.slots != null) {
            templateSlotRepository.deleteByTemplateId(id)

            val newSlots = request.slots.map { slotDto ->
                TemplateSlot(
                    templateId = savedTemplate.id!!,
                    dayOfWeek = DayOfWeek.of(slotDto.dayOfWeek),
                    startTime = LocalTime.parse(slotDto.startTime, timeFormatter),
                    endTime = LocalTime.parse(slotDto.endTime, timeFormatter),
                    durationMinutes = slotDto.durationMinutes
                )
            }
            templateSlotRepository.saveAll(newSlots)
        } else {
            templateSlotRepository.findByTemplateId(id)
        }

        val location = savedTemplate.locationId?.let { locationRepository.findById(it).orElse(null) }
        return toDTO(savedTemplate, slots, location)
    }

    @Transactional
    fun deleteTemplate(id: UUID) {
        if (!templateRepository.existsById(id)) {
            throw IllegalArgumentException("Template not found")
        }
        templateSlotRepository.deleteByTemplateId(id)
        templateRepository.deleteById(id)
    }

    private fun toDTO(
        template: SlotTemplate,
        slots: List<TemplateSlot>,
        location: TrainingLocation? = null
    ): SlotTemplateDTO {
        return SlotTemplateDTO(
            id = template.id.toString(),
            name = template.name,
            slots = slots.map { slot ->
                TemplateSlotDTO(
                    id = slot.id.toString(),
                    dayOfWeek = slot.dayOfWeek.value,
                    startTime = slot.startTime.format(timeFormatter),
                    endTime = slot.endTime.format(timeFormatter),
                    durationMinutes = slot.durationMinutes
                )
            }.sortedWith(compareBy({ it.dayOfWeek }, { it.startTime })),
            isActive = template.isActive,
            locationId = template.locationId?.toString(),
            locationName = location?.nameCs,
            locationColor = location?.color,
            createdAt = template.createdAt.toString()
        )
    }

    private fun buildLocationMap(templates: List<SlotTemplate>): Map<UUID, TrainingLocation> {
        val ids = templates.mapNotNull { it.locationId }.toSet()
        if (ids.isEmpty()) return emptyMap()
        return locationRepository.findAllById(ids).associateBy { it.id!! }
    }
}
