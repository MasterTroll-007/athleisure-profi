package com.fitness.repository

import com.fitness.entity.SlotTemplate
import com.fitness.entity.TemplateSlot
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface SlotTemplateRepository : JpaRepository<SlotTemplate, UUID> {

    fun findByIsActiveTrue(): List<SlotTemplate>

    fun findByName(name: String): SlotTemplate?
}

@Repository
interface TemplateSlotRepository : JpaRepository<TemplateSlot, UUID> {

    fun findByTemplateId(templateId: UUID): List<TemplateSlot>

    fun deleteByTemplateId(templateId: UUID)
}
