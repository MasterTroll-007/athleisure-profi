package com.fitness.repository

import com.fitness.entity.TrainingPlan
import com.fitness.entity.PurchasedPlan
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface TrainingPlanRepository : JpaRepository<TrainingPlan, UUID> {
    fun findByIsActiveTrueOrderBySortOrder(): List<TrainingPlan>
    fun findByIsActiveTrueOrderBySortOrder(pageable: Pageable): Page<TrainingPlan>
}

@Repository
interface PurchasedPlanRepository : JpaRepository<PurchasedPlan, UUID> {
    fun findByUserIdOrderByCreatedAtDesc(userId: UUID): List<PurchasedPlan>
    fun findByUserIdOrderByCreatedAtDesc(userId: UUID, pageable: Pageable): Page<PurchasedPlan>
    fun findByUserIdAndStatus(userId: UUID, status: String): List<PurchasedPlan>
    fun existsByUserIdAndPlanId(userId: UUID, planId: UUID): Boolean
}
