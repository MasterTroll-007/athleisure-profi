package com.fitness.service

import com.fitness.dto.*
import com.fitness.repository.*
import org.springframework.stereotype.Service
import java.util.*

@Service
class PlanService(
    private val trainingPlanRepository: TrainingPlanRepository,
    private val purchasedPlanRepository: PurchasedPlanRepository
) {

    fun getPlans(): List<TrainingPlanDTO> {
        return trainingPlanRepository.findByIsActiveTrueOrderBySortOrder()
            .map { plan ->
                TrainingPlanDTO(
                    id = plan.id.toString(),
                    name = plan.name,
                    description = plan.description,
                    credits = plan.credits,
                    price = plan.price,
                    currency = plan.currency,
                    validityDays = plan.validityDays,
                    sessionsCount = plan.sessionsCount,
                    isActive = plan.isActive
                )
            }
    }

    fun getUserPlans(userId: String): List<PurchasedPlanDTO> {
        return purchasedPlanRepository.findByUserIdOrderByCreatedAtDesc(UUID.fromString(userId))
            .map { purchased ->
                val plan = trainingPlanRepository.findById(purchased.planId).orElse(null)
                PurchasedPlanDTO(
                    id = purchased.id.toString(),
                    userId = purchased.userId.toString(),
                    planId = purchased.planId.toString(),
                    planName = plan?.name,
                    purchaseDate = purchased.purchaseDate.toString(),
                    expiryDate = purchased.expiryDate.toString(),
                    sessionsRemaining = purchased.sessionsRemaining,
                    status = purchased.status
                )
            }
    }
}
