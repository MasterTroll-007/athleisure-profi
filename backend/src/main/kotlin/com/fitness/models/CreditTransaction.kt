package com.fitness.models

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant
import java.util.*

object CreditTransactions : UUIDTable("credit_transactions") {
    val userId = reference("user_id", Users, onDelete = ReferenceOption.CASCADE)
    val amount = integer("amount")
    val type = varchar("type", 30)
    val referenceId = uuid("reference_id").nullable()
    val gopayPaymentId = varchar("gopay_payment_id", 255).nullable()
    val note = text("note").nullable()
    val createdAt = timestamp("created_at").default(Instant.now())
}

enum class TransactionType(val value: String) {
    PURCHASE("purchase"),
    RESERVATION("reservation"),
    PLAN_PURCHASE("plan_purchase"),
    ADMIN_ADJUSTMENT("admin_adjustment"),
    REFUND("refund")
}

@Serializable
data class CreditTransactionDTO(
    val id: String,
    val userId: String,
    val amount: Int,
    val type: String,
    val referenceId: String?,
    val gopayPaymentId: String?,
    val note: String?,
    val createdAt: String
)

@Serializable
data class CreditBalanceResponse(
    val balance: Int,
    val userId: String
)

@Serializable
data class AdminAdjustCreditsRequest(
    val userId: String,
    val amount: Int,
    val note: String? = null
)
