package com.fitness.entity

import jakarta.persistence.*
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.Instant
import java.util.*

@Entity
@Table(name = "availability_blocks")
data class AvailabilityBlock(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),

    val name: String? = null,

    @Column(name = "days_of_week", nullable = false)
    val daysOfWeek: String = "",

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week")
    val dayOfWeek: DayOfWeek? = null,

    @Column(name = "specific_date")
    val specificDate: LocalDate? = null,

    @Column(name = "start_time", nullable = false)
    val startTime: LocalTime,

    @Column(name = "end_time", nullable = false)
    val endTime: LocalTime,

    @Column(name = "slot_duration_minutes")
    val slotDurationMinutes: Int? = 60,

    @Column(name = "slot_duration")
    val slotDuration: Int? = null,

    @Column(name = "break_after_slots")
    val breakAfterSlots: Int? = null,

    @Column(name = "break_duration_minutes")
    val breakDurationMinutes: Int? = null,

    @Column(name = "is_recurring")
    val isRecurring: Boolean? = true,

    @Column(name = "is_blocked")
    val isBlocked: Boolean? = false,

    @Column(name = "is_active")
    val isActive: Boolean? = true,

    @Column(name = "created_at")
    val createdAt: Instant = Instant.now()
)
