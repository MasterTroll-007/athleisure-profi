package com.fitness.repositories

import com.fitness.models.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.math.BigDecimal
import java.time.Instant
import java.util.*

object CreditRepository {

    // Credit Packages
    fun findPackageById(id: UUID): CreditPackageDTO? = transaction {
        CreditPackages.select(CreditPackages.id eq id)
            .map { it.toPackageDTO() }
            .singleOrNull()
    }

    fun findAllPackages(): List<CreditPackageDTO> = transaction {
        CreditPackages.selectAll()
            .orderBy(CreditPackages.sortOrder, SortOrder.ASC)
            .map { it.toPackageDTO() }
    }

    fun findActivePackages(): List<CreditPackageDTO> = transaction {
        CreditPackages.select(CreditPackages.isActive eq true)
            .orderBy(CreditPackages.sortOrder, SortOrder.ASC)
            .map { it.toPackageDTO() }
    }

    fun createPackage(request: CreateCreditPackageRequest): CreditPackageDTO = transaction {
        val id = CreditPackages.insertAndGetId {
            it[nameCs] = request.nameCs
            it[nameEn] = request.nameEn
            it[credits] = request.credits
            it[bonusCredits] = request.bonusCredits
            it[priceCzk] = BigDecimal.valueOf(request.priceCzk)
            it[isActive] = request.isActive
            it[sortOrder] = request.sortOrder
            it[createdAt] = Instant.now()
        }

        findPackageById(id.value)!!
    }

    fun updatePackage(id: UUID, request: UpdateCreditPackageRequest): CreditPackageDTO? = transaction {
        CreditPackages.update({ CreditPackages.id eq id }) {
            request.nameCs?.let { value -> it[nameCs] = value }
            request.nameEn?.let { value -> it[nameEn] = value }
            request.credits?.let { value -> it[credits] = value }
            request.bonusCredits?.let { value -> it[bonusCredits] = value }
            request.priceCzk?.let { value -> it[priceCzk] = BigDecimal.valueOf(value) }
            request.isActive?.let { value -> it[isActive] = value }
            request.sortOrder?.let { value -> it[sortOrder] = value }
        }

        findPackageById(id)
    }

    fun deletePackage(id: UUID): Boolean = transaction {
        CreditPackages.deleteWhere { CreditPackages.id eq id } > 0
    }

    // Credit Transactions
    fun createTransaction(
        userId: UUID,
        amount: Int,
        type: String,
        referenceId: UUID? = null,
        gopayPaymentId: String? = null,
        note: String? = null
    ): CreditTransactionDTO = transaction {
        val id = CreditTransactions.insertAndGetId {
            it[CreditTransactions.userId] = userId
            it[CreditTransactions.amount] = amount
            it[CreditTransactions.type] = type
            it[CreditTransactions.referenceId] = referenceId
            it[CreditTransactions.gopayPaymentId] = gopayPaymentId
            it[CreditTransactions.note] = note
            it[createdAt] = Instant.now()
        }

        findTransactionById(id.value)!!
    }

    fun findTransactionById(id: UUID): CreditTransactionDTO? = transaction {
        CreditTransactions.select(CreditTransactions.id eq id)
            .map { it.toTransactionDTO() }
            .singleOrNull()
    }

    fun findTransactionsByUser(userId: UUID, limit: Int = 50): List<CreditTransactionDTO> = transaction {
        CreditTransactions.select(CreditTransactions.userId eq userId)
            .orderBy(CreditTransactions.createdAt, SortOrder.DESC)
            .limit(limit)
            .map { it.toTransactionDTO() }
    }

    fun findAllTransactions(limit: Int = 100): List<CreditTransactionDTO> = transaction {
        CreditTransactions.selectAll()
            .orderBy(CreditTransactions.createdAt, SortOrder.DESC)
            .limit(limit)
            .map { it.toTransactionDTO() }
    }

    // Pricing Items
    fun findPricingItemById(id: UUID): PricingItemDTO? = transaction {
        PricingItems.select(PricingItems.id eq id)
            .map { it.toPricingItemDTO() }
            .singleOrNull()
    }

    fun findAllPricingItems(): List<PricingItemDTO> = transaction {
        PricingItems.selectAll()
            .orderBy(PricingItems.sortOrder, SortOrder.ASC)
            .map { it.toPricingItemDTO() }
    }

    fun findActivePricingItems(): List<PricingItemDTO> = transaction {
        PricingItems.select(PricingItems.isActive eq true)
            .orderBy(PricingItems.sortOrder, SortOrder.ASC)
            .map { it.toPricingItemDTO() }
    }

    fun createPricingItem(request: CreatePricingItemRequest): PricingItemDTO = transaction {
        val id = PricingItems.insertAndGetId {
            it[nameCs] = request.nameCs
            it[nameEn] = request.nameEn
            it[descriptionCs] = request.descriptionCs
            it[descriptionEn] = request.descriptionEn
            it[credits] = request.credits
            it[isActive] = request.isActive
            it[sortOrder] = request.sortOrder
            it[createdAt] = Instant.now()
        }

        findPricingItemById(id.value)!!
    }

    fun updatePricingItem(id: UUID, request: UpdatePricingItemRequest): PricingItemDTO? = transaction {
        PricingItems.update({ PricingItems.id eq id }) {
            request.nameCs?.let { value -> it[nameCs] = value }
            request.nameEn?.let { value -> it[nameEn] = value }
            request.descriptionCs?.let { value -> it[descriptionCs] = value }
            request.descriptionEn?.let { value -> it[descriptionEn] = value }
            request.credits?.let { value -> it[credits] = value }
            request.isActive?.let { value -> it[isActive] = value }
            request.sortOrder?.let { value -> it[sortOrder] = value }
        }

        findPricingItemById(id)
    }

    fun deletePricingItem(id: UUID): Boolean = transaction {
        PricingItems.deleteWhere { PricingItems.id eq id } > 0
    }

    private fun ResultRow.toPackageDTO() = CreditPackageDTO(
        id = this[CreditPackages.id].value.toString(),
        nameCs = this[CreditPackages.nameCs],
        nameEn = this[CreditPackages.nameEn],
        credits = this[CreditPackages.credits],
        bonusCredits = this[CreditPackages.bonusCredits],
        priceCzk = this[CreditPackages.priceCzk].toDouble(),
        isActive = this[CreditPackages.isActive],
        sortOrder = this[CreditPackages.sortOrder]
    )

    private fun ResultRow.toTransactionDTO() = CreditTransactionDTO(
        id = this[CreditTransactions.id].value.toString(),
        userId = this[CreditTransactions.userId].value.toString(),
        amount = this[CreditTransactions.amount],
        type = this[CreditTransactions.type],
        referenceId = this[CreditTransactions.referenceId]?.toString(),
        gopayPaymentId = this[CreditTransactions.gopayPaymentId],
        note = this[CreditTransactions.note],
        createdAt = this[CreditTransactions.createdAt].toString()
    )

    private fun ResultRow.toPricingItemDTO() = PricingItemDTO(
        id = this[PricingItems.id].value.toString(),
        nameCs = this[PricingItems.nameCs],
        nameEn = this[PricingItems.nameEn],
        descriptionCs = this[PricingItems.descriptionCs],
        descriptionEn = this[PricingItems.descriptionEn],
        credits = this[PricingItems.credits],
        isActive = this[PricingItems.isActive],
        sortOrder = this[PricingItems.sortOrder]
    )
}
