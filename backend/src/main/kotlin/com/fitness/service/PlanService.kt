package com.fitness.service

import com.fitness.dto.*
import com.fitness.entity.CreditTransaction
import com.fitness.entity.PurchasedPlan
import com.fitness.entity.TransactionType
import com.fitness.repository.*
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

@Service
class PlanService(
    private val trainingPlanRepository: TrainingPlanRepository,
    private val purchasedPlanRepository: PurchasedPlanRepository,
    private val userRepository: UserRepository,
    private val creditTransactionRepository: CreditTransactionRepository
) {

    @Cacheable("trainingPlans")
    fun getPlans(): List<TrainingPlanDTO> {
        return trainingPlanRepository.findByIsActiveTrueOrderBySortOrder()
            .map { plan ->
                TrainingPlanDTO(
                    id = plan.id.toString(),
                    name = plan.nameCs,
                    description = plan.descriptionCs,
                    credits = plan.credits,
                    price = plan.price,
                    currency = plan.currency,
                    validityDays = plan.validityDays,
                    sessionsCount = plan.sessionsCount,
                    isActive = plan.isActive
                )
            }
    }

    fun getPlanDetail(planId: String): PlanDetailDTO {
        val plan = trainingPlanRepository.findById(UUID.fromString(planId))
            .orElseThrow { NoSuchElementException("Plan not found") }

        return PlanDetailDTO(
            id = plan.id.toString(),
            name = plan.nameCs,
            nameCs = plan.nameCs,
            nameEn = plan.nameEn,
            description = plan.descriptionCs,
            descriptionCs = plan.descriptionCs,
            descriptionEn = plan.descriptionEn,
            credits = plan.credits,
            price = plan.price,
            currency = plan.currency,
            validityDays = plan.validityDays,
            sessionsCount = plan.sessionsCount,
            isActive = plan.isActive,
            hasFile = plan.filePath != null,
            previewImage = plan.previewImage
        )
    }

    @Transactional
    fun purchasePlan(userId: String, planId: String): PurchasePlanResponse {
        val userUUID = UUID.fromString(userId)
        val planUUID = UUID.fromString(planId)

        val user = userRepository.findById(userUUID)
            .orElseThrow { NoSuchElementException("User not found") }

        val plan = trainingPlanRepository.findById(planUUID)
            .orElseThrow { NoSuchElementException("Plan not found") }

        if (!plan.isActive) {
            throw IllegalArgumentException("This plan is no longer available")
        }

        // Check if already purchased
        if (purchasedPlanRepository.existsByUserIdAndPlanId(userUUID, planUUID)) {
            throw IllegalArgumentException("You have already purchased this plan")
        }

        // Atomically deduct credits (prevents race condition)
        val rowsUpdated = userRepository.deductCreditsIfSufficient(userUUID, plan.credits)
        if (rowsUpdated == 0) {
            throw IllegalArgumentException("Not enough credits")
        }

        // Record transaction
        val purchasedPlan = purchasedPlanRepository.save(
            PurchasedPlan(
                userId = userUUID,
                planId = planUUID,
                expiryDate = LocalDate.now().plusDays(plan.validityDays.toLong()),
                sessionsRemaining = plan.sessionsCount
            )
        )

        creditTransactionRepository.save(
            CreditTransaction(
                userId = userUUID,
                amount = -plan.credits,
                type = TransactionType.PLAN_PURCHASE.value,
                referenceId = purchasedPlan.id,
                note = "Nákup plánu: ${plan.nameCs}"
            )
        )

        val newBalance = user.credits - plan.credits

        val dto = PurchasedPlanDTO(
            id = purchasedPlan.id.toString(),
            userId = purchasedPlan.userId.toString(),
            planId = purchasedPlan.planId.toString(),
            planName = plan.nameCs,
            purchaseDate = purchasedPlan.purchaseDate.toString(),
            expiryDate = purchasedPlan.expiryDate.toString(),
            sessionsRemaining = purchasedPlan.sessionsRemaining,
            status = purchasedPlan.status
        )

        return PurchasePlanResponse(
            purchasedPlan = dto,
            newBalance = newBalance
        )
    }

    fun checkPurchase(userId: String, planId: String): CheckPurchaseResponse {
        val purchased = purchasedPlanRepository.existsByUserIdAndPlanId(
            UUID.fromString(userId),
            UUID.fromString(planId)
        )
        return CheckPurchaseResponse(purchased = purchased)
    }

    fun getUserPlans(userId: String): List<PurchasedPlanDTO> {
        val purchases = purchasedPlanRepository.findByUserIdOrderByCreatedAtDesc(UUID.fromString(userId))
        // Batch fetch all plans to avoid N+1
        val planIds = purchases.map { it.planId }.toSet()
        val plansMap = if (planIds.isNotEmpty()) {
            trainingPlanRepository.findAllById(planIds).associateBy { it.id }
        } else emptyMap()

        return purchases.map { purchased ->
            val plan = plansMap[purchased.planId]
            PurchasedPlanDTO(
                id = purchased.id.toString(),
                userId = purchased.userId.toString(),
                planId = purchased.planId.toString(),
                planName = plan?.nameCs,
                purchaseDate = purchased.purchaseDate.toString(),
                expiryDate = purchased.expiryDate.toString(),
                sessionsRemaining = purchased.sessionsRemaining,
                status = purchased.status
            )
        }
    }
}
