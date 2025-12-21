package com.fitness.models

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant

object PurchasedPlans : UUIDTable("purchased_plans") {
    val userId = reference("user_id", Users, onDelete = ReferenceOption.CASCADE)
    val planId = reference("plan_id", TrainingPlans, onDelete = ReferenceOption.CASCADE)
    val creditsUsed = integer("credits_used")
    val purchasedAt = timestamp("purchased_at").default(Instant.now())
}

@Serializable
data class PurchasedPlanDTO(
    val id: String,
    val userId: String,
    val planId: String,
    val planName: String,
    val creditsUsed: Int,
    val purchasedAt: String
)

@Serializable
data class PurchasePlanRequest(
    val planId: String
)
