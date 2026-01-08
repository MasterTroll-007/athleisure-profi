package com.fitness.repository

import com.fitness.entity.ClientNote
import com.fitness.entity.GopayPayment
import com.fitness.entity.PricingItem
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
