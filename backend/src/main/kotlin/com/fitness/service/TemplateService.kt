package com.fitness.service

import com.fitness.dto.*
import com.fitness.entity.SlotTemplate
import com.fitness.entity.TemplateSlot
import com.fitness.repository.SlotTemplateRepository
import com.fitness.repository.TemplateSlotRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.*

@Service
class TemplateService(
    private val templateRepository: SlotTemplateRepository,
    private val templateSlotRepository: TemplateSlotRepository
) {

    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    fun getAllTemplates(): List<SlotTemplateDTO> {
        return templateRepository.findAll().map { template ->
            val slots = templateSlotRepository.findByTemplateId(template.id)
            toDTO(template, slots)
        }
    }

    fun getActiveTemplates(): List<SlotTemplateDTO> {
        return templateRepository.findByIsActiveTrue().map { template ->
            val slots = templateSlotRepository.findByTemplateId(template.id)
            toDTO(template, slots)
        }
    }

    fun getTemplate(id: UUID): SlotTemplateDTO {
        val template = templateRepository.findById(id)
            .orElseThrow { IllegalArgumentException("Template not found") }
        val slots = templateSlotRepository.findByTemplateId(template.id)
        return toDTO(template, slots)
    }

    @Transactional
    fun createTemplate(request: CreateTemplateRequest): SlotTemplateDTO {
        val template = SlotTemplate(name = request.name)
        val savedTemplate = templateRepository.save(template)

        val slots = request.slots.map { slotDto ->
            TemplateSlot(
                templateId = savedTemplate.id,
                dayOfWeek = DayOfWeek.of(slotDto.dayOfWeek),
                startTime = LocalTime.parse(slotDto.startTime, timeFormatter),
                endTime = LocalTime.parse(slotDto.endTime, timeFormatter),
                durationMinutes = slotDto.durationMinutes
            )
        }

        val savedSlots = templateSlotRepository.saveAll(slots)
        return toDTO(savedTemplate, savedSlots)
    }

    @Transactional
    fun updateTemplate(id: UUID, request: UpdateTemplateRequest): SlotTemplateDTO {
        val template = templateRepository.findById(id)
            .orElseThrow { IllegalArgumentException("Template not found") }

        request.name?.let { template.name = it }
        request.isActive?.let { template.isActive = it }

        val savedTemplate = templateRepository.save(template)

        // If slots are provided, replace all existing slots
        val slots = if (request.slots != null) {
            templateSlotRepository.deleteByTemplateId(id)

            val newSlots = request.slots.map { slotDto ->
                TemplateSlot(
                    templateId = savedTemplate.id,
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

        return toDTO(savedTemplate, slots)
    }

    @Transactional
    fun deleteTemplate(id: UUID) {
        if (!templateRepository.existsById(id)) {
            throw IllegalArgumentException("Template not found")
        }
        templateSlotRepository.deleteByTemplateId(id)
        templateRepository.deleteById(id)
    }

    private fun toDTO(template: SlotTemplate, slots: List<TemplateSlot>): SlotTemplateDTO {
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
            createdAt = template.createdAt.toString()
        )
    }
}
