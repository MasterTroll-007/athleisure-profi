package com.fitness.repositories

import com.fitness.models.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.math.BigDecimal
import java.time.Instant
import java.util.*

object GopayRepository {

    fun findById(id: UUID): GopayPaymentDTO? = transaction {
        (GopayPayments innerJoin Users)
            .leftJoin(CreditPackages)
            .select(GopayPayments.id eq id)
            .map { it.toDTO() }
            .singleOrNull()
    }

    fun findByGopayId(gopayId: Long): GopayPaymentDTO? = transaction {
        (GopayPayments innerJoin Users)
            .leftJoin(CreditPackages)
            .select(GopayPayments.gopayId eq gopayId)
            .map { it.toDTO() }
            .singleOrNull()
    }

    fun create(
        userId: UUID,
        amount: Double,
        state: String,
        creditPackageId: UUID?,
        gopayId: Long? = null
    ): GopayPaymentDTO = transaction {
        val id = GopayPayments.insertAndGetId {
            it[GopayPayments.userId] = userId
            it[GopayPayments.gopayId] = gopayId
            it[GopayPayments.amount] = BigDecimal.valueOf(amount)
            it[GopayPayments.state] = state
            it[GopayPayments.creditPackageId] = creditPackageId
            it[createdAt] = Instant.now()
            it[updatedAt] = Instant.now()
        }

        findById(id.value)!!
    }

    fun updateState(id: UUID, state: String, gopayId: Long? = null): Boolean = transaction {
        GopayPayments.update({ GopayPayments.id eq id }) {
            it[GopayPayments.state] = state
            gopayId?.let { gid -> it[GopayPayments.gopayId] = gid }
            it[updatedAt] = Instant.now()
        } > 0
    }

    fun findAll(limit: Int = 100): List<GopayPaymentDTO> = transaction {
        (GopayPayments innerJoin Users)
            .leftJoin(CreditPackages)
            .selectAll()
            .orderBy(GopayPayments.createdAt, SortOrder.DESC)
            .limit(limit)
            .map { it.toDTO() }
    }

    fun findByUser(userId: UUID): List<GopayPaymentDTO> = transaction {
        (GopayPayments innerJoin Users)
            .leftJoin(CreditPackages)
            .select(GopayPayments.userId eq userId)
            .orderBy(GopayPayments.createdAt, SortOrder.DESC)
            .map { it.toDTO() }
    }

    fun findByState(state: String): List<GopayPaymentDTO> = transaction {
        (GopayPayments innerJoin Users)
            .leftJoin(CreditPackages)
            .select(GopayPayments.state eq state)
            .orderBy(GopayPayments.createdAt, SortOrder.DESC)
            .map { it.toDTO() }
    }

    private fun ResultRow.toDTO(): GopayPaymentDTO {
        val firstName = this[Users.firstName] ?: ""
        val lastName = this[Users.lastName] ?: ""

        return GopayPaymentDTO(
            id = this[GopayPayments.id].value.toString(),
            userId = this[GopayPayments.userId].value.toString(),
            userName = "$firstName $lastName".trim().ifEmpty { this[Users.email] },
            gopayId = this[GopayPayments.gopayId],
            amount = this[GopayPayments.amount].toDouble(),
            currency = this[GopayPayments.currency],
            state = this[GopayPayments.state],
            creditPackageId = this[GopayPayments.creditPackageId]?.value?.toString(),
            creditPackageName = this.getOrNull(CreditPackages.nameCs),
            createdAt = this[GopayPayments.createdAt].toString(),
            updatedAt = this[GopayPayments.updatedAt].toString()
        )
    }
}
