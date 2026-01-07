package com.fitness.repository

import com.fitness.entity.CreditPackage
import com.fitness.entity.CreditTransaction
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
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
}
