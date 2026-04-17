package com.fitness.entity

import jakarta.persistence.*
import java.util.*

@Entity
@Table(
    name = "template_slot_pricing_items",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_template_slot_pricing_item", columnNames = ["template_slot_id", "pricing_item_id"])
    ],
    indexes = [
        Index(name = "idx_tspi_template_slot", columnList = "template_slot_id"),
        Index(name = "idx_tspi_pricing_item", columnList = "pricing_item_id")
    ]
)
data class TemplatePricingItem(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @Column(name = "template_slot_id", nullable = false)
    val templateSlotId: UUID,

    @Column(name = "pricing_item_id", nullable = false)
    val pricingItemId: UUID
)
