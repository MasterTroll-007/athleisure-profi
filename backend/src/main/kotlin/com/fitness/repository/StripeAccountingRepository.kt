package com.fitness.repository

import com.fitness.entity.StripeBalanceTransaction
import com.fitness.entity.StripePayout
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.*

@Repository
interface StripeBalanceTransactionRepository : JpaRepository<StripeBalanceTransaction, UUID> {
    fun findByStripeBalanceTransactionId(stripeBalanceTransactionId: String): StripeBalanceTransaction?
    fun findByStripeBalanceTransactionIdIn(stripeBalanceTransactionIds: Collection<String>): List<StripeBalanceTransaction>

    @Query("""
        SELECT b FROM StripeBalanceTransaction b
        WHERE b.createdAtStripe >= :from AND b.createdAtStripe < :to
        ORDER BY b.createdAtStripe ASC
    """)
    fun findByCreatedAtStripeBetween(from: Instant, to: Instant): List<StripeBalanceTransaction>

    @Query("""
        SELECT b FROM StripeBalanceTransaction b
        WHERE b.stripeBalanceTransactionId IN :ids
        ORDER BY b.createdAtStripe ASC
    """)
    fun findByIdsForExport(ids: Collection<String>): List<StripeBalanceTransaction>
}

@Repository
interface StripePayoutRepository : JpaRepository<StripePayout, UUID> {
    fun findByStripePayoutId(stripePayoutId: String): StripePayout?

    @Query("""
        SELECT p FROM StripePayout p
        WHERE p.createdAtStripe >= :from AND p.createdAtStripe < :to
        ORDER BY p.createdAtStripe ASC
    """)
    fun findByCreatedAtStripeBetween(from: Instant, to: Instant): List<StripePayout>
}
