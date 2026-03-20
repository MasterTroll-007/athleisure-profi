package com.fitness.repository

import com.fitness.entity.CreditPackage
import com.fitness.entity.CreditTransaction
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

    @Query("SELECT t FROM CreditTransaction t WHERE t.expiresAt IS NOT NULL AND t.expiresAt <= :now AND t.amount > 0 AND t.type = 'purchase'")
    fun findExpiredTransactions(now: Instant): List<CreditTransaction>
}
