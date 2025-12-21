package com.fitness.models

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant

object GopayPayments : UUIDTable("gopay_payments") {
    val userId = reference("user_id", Users, onDelete = ReferenceOption.CASCADE)
    val gopayId = long("gopay_id").nullable()
    val amount = decimal("amount", 10, 2)
    val currency = varchar("currency", 3).default("CZK")
    val state = varchar("state", 30)
    val creditPackageId = reference("credit_package_id", CreditPackages, onDelete = ReferenceOption.SET_NULL).nullable()
    val createdAt = timestamp("created_at").default(Instant.now())
    val updatedAt = timestamp("updated_at").default(Instant.now())
}

enum class GopayState(val value: String) {
    CREATED("CREATED"),
    PAYMENT_METHOD_CHOSEN("PAYMENT_METHOD_CHOSEN"),
    PAID("PAID"),
    AUTHORIZED("AUTHORIZED"),
    CANCELED("CANCELED"),
    TIMEOUTED("TIMEOUTED"),
    REFUNDED("REFUNDED")
}

@Serializable
data class GopayPaymentDTO(
    val id: String,
    val userId: String,
    val userName: String?,
    val gopayId: Long?,
    val amount: Double,
    val currency: String,
    val state: String,
    val creditPackageId: String?,
    val creditPackageName: String?,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class CreatePaymentRequest(
    val packageId: String
)

@Serializable
data class PaymentResponse(
    val paymentId: String,
    val gwUrl: String?
)
