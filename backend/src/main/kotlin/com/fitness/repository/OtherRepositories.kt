package com.fitness.repository

import com.fitness.entity.CancellationPolicy
import com.fitness.entity.ClientNote
import com.fitness.entity.GopayPayment
import com.fitness.entity.PricingItem
import com.fitness.entity.SlotPricingItem
import com.fitness.entity.TemplatePricingItem
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface PricingItemRepository : JpaRepository<PricingItem, UUID> {
    fun findByIsActiveTrueOrderBySortOrder(): List<PricingItem>
    fun findByAdminIdAndIsActiveTrueOrderBySortOrder(adminId: UUID): List<PricingItem>
    fun findByAdminIdOrderBySortOrder(adminId: UUID): List<PricingItem>
}

@Repository
interface ClientNoteRepository : JpaRepository<ClientNote, UUID> {
    fun findByClientIdOrderByCreatedAtDesc(clientId: UUID): List<ClientNote>
}

@Repository
interface GopayPaymentRepository : JpaRepository<GopayPayment, UUID> {
    fun findByGopayId(gopayId: String): GopayPayment?
    fun findByUserIdOrderByCreatedAtDesc(userId: UUID): List<GopayPayment>
}

@Repository
interface CancellationPolicyRepository : JpaRepository<CancellationPolicy, UUID> {
    fun findByTrainerId(trainerId: UUID): CancellationPolicy?
}

@Repository
interface SlotPricingItemRepository : JpaRepository<SlotPricingItem, UUID> {
    fun findBySlotId(slotId: UUID): List<SlotPricingItem>
    fun findBySlotIdIn(slotIds: List<UUID>): List<SlotPricingItem>
    fun deleteBySlotId(slotId: UUID)
}

@Repository
interface TemplatePricingItemRepository : JpaRepository<TemplatePricingItem, UUID> {
    fun findByTemplateSlotId(templateSlotId: UUID): List<TemplatePricingItem>
    fun findByTemplateSlotIdIn(templateSlotIds: List<UUID>): List<TemplatePricingItem>
    fun deleteByTemplateSlotId(templateSlotId: UUID)
}
