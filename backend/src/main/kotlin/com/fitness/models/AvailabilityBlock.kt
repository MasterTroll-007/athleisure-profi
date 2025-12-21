package com.fitness.models

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.time
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant

object AvailabilityBlocks : UUIDTable("availability_blocks") {
    val name = varchar("name", 100).nullable()
    val daysOfWeek = varchar("days_of_week", 50) // Stored as comma-separated: "1,2,3,4,5"
    val startTime = time("start_time")
    val endTime = time("end_time")
    val slotDurationMinutes = integer("slot_duration_minutes").default(60)
    val breakAfterSlots = integer("break_after_slots").nullable()
    val breakDurationMinutes = integer("break_duration_minutes").nullable()
    val isActive = bool("is_active").default(true)
    val createdAt = timestamp("created_at").default(Instant.now())
}

@Serializable
data class AvailabilityBlockDTO(
    val id: String,
    val name: String?,
    val daysOfWeek: List<Int>,
    val startTime: String,
    val endTime: String,
    val slotDurationMinutes: Int,
    val breakAfterSlots: Int?,
    val breakDurationMinutes: Int?,
    val isActive: Boolean,
    val createdAt: String
)

@Serializable
data class CreateAvailabilityBlockRequest(
    val name: String? = null,
    val daysOfWeek: List<Int>,
    val startTime: String,
    val endTime: String,
    val slotDurationMinutes: Int = 60,
    val breakAfterSlots: Int? = null,
    val breakDurationMinutes: Int? = null,
    val isActive: Boolean = true
)

@Serializable
data class UpdateAvailabilityBlockRequest(
    val name: String? = null,
    val daysOfWeek: List<Int>? = null,
    val startTime: String? = null,
    val endTime: String? = null,
    val slotDurationMinutes: Int? = null,
    val breakAfterSlots: Int? = null,
    val breakDurationMinutes: Int? = null,
    val isActive: Boolean? = null
)

@Serializable
data class AvailableSlot(
    val start: String,
    val end: String,
    val blockId: String
)

@Serializable
data class AvailableSlotsResponse(
    val date: String,
    val slots: List<AvailableSlot>
)
