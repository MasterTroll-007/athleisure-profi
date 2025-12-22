package com.fitness.repository

import com.fitness.entity.TrainingPlan
import com.fitness.entity.PurchasedPlan
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface TrainingPlanRepository : JpaRepository<TrainingPlan, UUID> {
    fun findByIsActiveTrueOrderBySortOrder(): List<TrainingPlan>
}

@Repository
interface PurchasedPlanRepository : JpaRepository<PurchasedPlan, UUID> {
    fun findByUserIdOrderByCreatedAtDesc(userId: UUID): List<PurchasedPlan>
    fun findByUserIdAndStatus(userId: UUID, status: String): List<PurchasedPlan>
}
