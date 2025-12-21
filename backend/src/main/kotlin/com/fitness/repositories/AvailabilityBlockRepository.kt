package com.fitness.repositories

import com.fitness.models.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.time.LocalTime
import java.util.*

object AvailabilityBlockRepository {

    fun findById(id: UUID): AvailabilityBlockDTO? = transaction {
        AvailabilityBlocks.select(AvailabilityBlocks.id eq id)
            .map { it.toDTO() }
            .singleOrNull()
    }

    fun findAll(): List<AvailabilityBlockDTO> = transaction {
        AvailabilityBlocks.selectAll()
            .orderBy(AvailabilityBlocks.createdAt, SortOrder.DESC)
            .map { it.toDTO() }
    }

    fun findActive(): List<AvailabilityBlockDTO> = transaction {
        AvailabilityBlocks.select(AvailabilityBlocks.isActive eq true)
            .orderBy(AvailabilityBlocks.startTime, SortOrder.ASC)
            .map { it.toDTO() }
    }

    fun findActiveForDay(dayOfWeek: Int): List<AvailabilityBlockData> = transaction {
        AvailabilityBlocks.select(AvailabilityBlocks.isActive eq true)
            .filter { row ->
                val days = parseDaysOfWeek(row[AvailabilityBlocks.daysOfWeek])
                days.contains(dayOfWeek)
            }
            .map { it.toData() }
    }

    private fun parseDaysOfWeek(value: String): List<Int> {
        return if (value.isBlank()) emptyList()
        else value.split(",").mapNotNull { it.trim().toIntOrNull() }
    }

    private fun formatDaysOfWeek(days: List<Int>): String {
        return days.joinToString(",")
    }

    fun create(request: CreateAvailabilityBlockRequest): AvailabilityBlockDTO = transaction {
        val id = AvailabilityBlocks.insertAndGetId {
            it[name] = request.name
            it[daysOfWeek] = formatDaysOfWeek(request.daysOfWeek)
            it[startTime] = LocalTime.parse(request.startTime)
            it[endTime] = LocalTime.parse(request.endTime)
            it[slotDurationMinutes] = request.slotDurationMinutes
            it[breakAfterSlots] = request.breakAfterSlots
            it[breakDurationMinutes] = request.breakDurationMinutes
            it[isActive] = request.isActive
            it[createdAt] = Instant.now()
        }

        findById(id.value)!!
    }

    fun update(id: UUID, request: UpdateAvailabilityBlockRequest): AvailabilityBlockDTO? = transaction {
        AvailabilityBlocks.update({ AvailabilityBlocks.id eq id }) {
            request.name?.let { value -> it[name] = value }
            request.daysOfWeek?.let { value -> it[daysOfWeek] = formatDaysOfWeek(value) }
            request.startTime?.let { value -> it[startTime] = LocalTime.parse(value) }
            request.endTime?.let { value -> it[endTime] = LocalTime.parse(value) }
            request.slotDurationMinutes?.let { value -> it[slotDurationMinutes] = value }
            request.breakAfterSlots?.let { value -> it[breakAfterSlots] = value }
            request.breakDurationMinutes?.let { value -> it[breakDurationMinutes] = value }
            request.isActive?.let { value -> it[isActive] = value }
        }

        findById(id)
    }

    fun delete(id: UUID): Boolean = transaction {
        AvailabilityBlocks.deleteWhere { AvailabilityBlocks.id eq id } > 0
    }

    fun setActive(id: UUID, active: Boolean): Boolean = transaction {
        AvailabilityBlocks.update({ AvailabilityBlocks.id eq id }) {
            it[isActive] = active
        } > 0
    }

    private fun ResultRow.toDTO() = AvailabilityBlockDTO(
        id = this[AvailabilityBlocks.id].value.toString(),
        name = this[AvailabilityBlocks.name],
        daysOfWeek = parseDaysOfWeek(this[AvailabilityBlocks.daysOfWeek]),
        startTime = this[AvailabilityBlocks.startTime].toString(),
        endTime = this[AvailabilityBlocks.endTime].toString(),
        slotDurationMinutes = this[AvailabilityBlocks.slotDurationMinutes],
        breakAfterSlots = this[AvailabilityBlocks.breakAfterSlots],
        breakDurationMinutes = this[AvailabilityBlocks.breakDurationMinutes],
        isActive = this[AvailabilityBlocks.isActive],
        createdAt = this[AvailabilityBlocks.createdAt].toString()
    )

    private fun ResultRow.toData() = AvailabilityBlockData(
        id = this[AvailabilityBlocks.id].value,
        name = this[AvailabilityBlocks.name],
        daysOfWeek = parseDaysOfWeek(this[AvailabilityBlocks.daysOfWeek]),
        startTime = this[AvailabilityBlocks.startTime],
        endTime = this[AvailabilityBlocks.endTime],
        slotDurationMinutes = this[AvailabilityBlocks.slotDurationMinutes],
        breakAfterSlots = this[AvailabilityBlocks.breakAfterSlots],
        breakDurationMinutes = this[AvailabilityBlocks.breakDurationMinutes]
    )
}

data class AvailabilityBlockData(
    val id: UUID,
    val name: String?,
    val daysOfWeek: List<Int>,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val slotDurationMinutes: Int,
    val breakAfterSlots: Int?,
    val breakDurationMinutes: Int?
)
