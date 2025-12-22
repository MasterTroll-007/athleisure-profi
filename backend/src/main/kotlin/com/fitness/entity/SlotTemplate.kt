package com.fitness.entity

import jakarta.persistence.*
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalTime
import java.util.*

@Entity
@Table(name = "slot_templates")
data class SlotTemplate(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),

    @Column(nullable = false)
    var name: String,

    @Column(name = "is_active")
    var isActive: Boolean = true,

    @Column(name = "created_at")
    val createdAt: Instant = Instant.now()
)

@Entity
@Table(name = "template_slots")
data class TemplateSlot(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),

    @Column(name = "template_id", nullable = false)
    val templateId: UUID,

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false)
    val dayOfWeek: DayOfWeek,

    @Column(name = "start_time", nullable = false)
    val startTime: LocalTime,

    @Column(name = "end_time", nullable = false)
    val endTime: LocalTime,

    @Column(name = "duration_minutes", nullable = false)
    val durationMinutes: Int = 60
)
