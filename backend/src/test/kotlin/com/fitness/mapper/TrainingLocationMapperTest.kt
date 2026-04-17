package com.fitness.mapper

import com.fitness.entity.TrainingLocation
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class TrainingLocationMapperTest {

    private val mapper = TrainingLocationMapper()

    @Test
    fun `toDTO copies all bilingual fields and color`() {
        val id = UUID.randomUUID()
        val now = Instant.parse("2026-04-17T10:15:30Z")
        val entity = TrainingLocation(
            id = id,
            nameCs = "Gym Praha",
            nameEn = "Gym Prague",
            addressCs = "Václavské nám. 1",
            addressEn = "Wenceslas Sq. 1",
            color = "#3B82F6",
            isActive = true,
            adminId = UUID.randomUUID(),
            createdAt = now,
        )

        val dto = mapper.toDTO(entity)

        assertEquals(id.toString(), dto.id)
        assertEquals("Gym Praha", dto.nameCs)
        assertEquals("Gym Prague", dto.nameEn)
        assertEquals("Václavské nám. 1", dto.addressCs)
        assertEquals("Wenceslas Sq. 1", dto.addressEn)
        assertEquals("#3B82F6", dto.color)
        assertEquals(true, dto.isActive)
        assertEquals(now.toString(), dto.createdAt)
    }

    @Test
    fun `toDTO preserves nullable EN fields`() {
        val entity = TrainingLocation(
            id = UUID.randomUUID(),
            nameCs = "A",
            nameEn = null,
            addressCs = null,
            addressEn = null,
            color = "#10B981",
        )
        val dto = mapper.toDTO(entity)
        assertNull(dto.nameEn)
        assertNull(dto.addressCs)
        assertNull(dto.addressEn)
    }

    @Test
    fun `toDTOBatch preserves order`() {
        val a = TrainingLocation(id = UUID.randomUUID(), nameCs = "A", color = "#111111")
        val b = TrainingLocation(id = UUID.randomUUID(), nameCs = "B", color = "#222222")
        val c = TrainingLocation(id = UUID.randomUUID(), nameCs = "C", color = "#333333")

        val dtos = mapper.toDTOBatch(listOf(a, b, c))

        assertEquals(listOf("A", "B", "C"), dtos.map { it.nameCs })
    }

    @Test
    fun `toDTOBatch on empty list returns empty`() {
        assertEquals(emptyList<Any>(), mapper.toDTOBatch(emptyList()))
    }
}
