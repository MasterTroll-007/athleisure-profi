package com.fitness.repository

import com.fitness.entity.CreditExpirationNotification
import com.fitness.entity.CreditPackage
import com.fitness.entity.CreditTransaction
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.*

@Repository
interface CreditPackageRepository : JpaRepository<CreditPackage, UUID> {
    fun findByIsActiveTrueOrderBySortOrder(): List<CreditPackage>
    fun findByTrainerIdAndIsActiveTrueOrderBySortOrder(trainerId: UUID): List<CreditPackage>
    fun findByTrainerIdOrderBySortOrder(trainerId: UUID): List<CreditPackage>
}

@Repository
interface CreditTransactionRepository : JpaRepository<CreditTransaction, UUID> {
    fun findByUserIdOrderByCreatedAtDesc(userId: UUID): List<CreditTransaction>
    fun findByUserIdOrderByCreatedAtDesc(userId: UUID, pageable: Pageable): Page<CreditTransaction>

    @Query("SELECT t FROM CreditTransaction t WHERE t.expiresAt IS NOT NULL AND t.expiresAt <= :now AND t.amount > 0 AND t.type = 'purchase'")
    fun findExpiredTransactions(now: Instant): List<CreditTransaction>

    @Query("SELECT t FROM CreditTransaction t WHERE t.expiresAt IS NOT NULL AND t.expiresAt BETWEEN :from AND :to AND t.amount > 0 AND t.type = 'purchase'")
    fun findExpiringBetween(from: Instant, to: Instant): List<CreditTransaction>

    @Query("SELECT SUM(t.amount) FROM CreditTransaction t WHERE t.type = :type AND t.createdAt BETWEEN :from AND :to")
    fun sumAmountByTypeAndDateRange(type: String, from: Instant, to: Instant): Long?
}

@Repository
interface CreditExpirationNotificationRepository : JpaRepository<CreditExpirationNotification, UUID> {
    @Query("SELECT COUNT(n) > 0 FROM CreditExpirationNotification n WHERE n.transactionId = :transactionId AND n.daysBefore = :daysBefore")
    fun existsByTransactionIdAndDaysBeforeValue(transactionId: UUID, daysBefore: Int): Boolean
}
