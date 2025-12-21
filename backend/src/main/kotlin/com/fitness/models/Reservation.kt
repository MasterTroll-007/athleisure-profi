package com.fitness.models

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.javatime.time
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant

object Reservations : UUIDTable("reservations") {
    val userId = reference("user_id", Users, onDelete = ReferenceOption.CASCADE)
    val blockId = reference("block_id", AvailabilityBlocks, onDelete = ReferenceOption.SET_NULL).nullable()
    val date = date("date")
    val startTime = time("start_time")
    val endTime = time("end_time")
    val status = varchar("status", 20).default("confirmed")
    val creditsUsed = integer("credits_used").default(1)
    val pricingItemId = reference("pricing_item_id", PricingItems, onDelete = ReferenceOption.SET_NULL).nullable()
    val createdAt = timestamp("created_at").default(Instant.now())
    val cancelledAt = timestamp("cancelled_at").nullable()
}

@Serializable
data class ReservationDTO(
    val id: String,
    val userId: String,
    val userName: String?,
    val userEmail: String?,
    val blockId: String?,
    val date: String,
    val startTime: String,
    val endTime: String,
    val status: String,
    val creditsUsed: Int,
    val pricingItemId: String?,
    val pricingItemName: String?,
    val createdAt: String,
    val cancelledAt: String?
)

@Serializable
data class CreateReservationRequest(
    val date: String,
    val startTime: String,
    val endTime: String,
    val blockId: String,
    val pricingItemId: String? = null
)

@Serializable
data class ReservationCalendarEvent(
    val id: String,
    val title: String,
    val start: String,
    val end: String,
    val status: String,
    val clientName: String?,
    val clientEmail: String?
)
