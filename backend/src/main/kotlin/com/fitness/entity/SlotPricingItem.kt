package com.fitness.entity

import jakarta.persistence.*
import java.util.*

@Entity
@Table(
    name = "slot_pricing_items",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_slot_pricing_item", columnNames = ["slot_id", "pricing_item_id"])
    ],
    indexes = [
        Index(name = "idx_spi_slot", columnList = "slot_id"),
        Index(name = "idx_spi_pricing_item", columnList = "pricing_item_id")
    ]
)
data class SlotPricingItem(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),

    @Column(name = "slot_id", nullable = false)
    val slotId: UUID,

    @Column(name = "pricing_item_id", nullable = false)
    val pricingItemId: UUID
)
