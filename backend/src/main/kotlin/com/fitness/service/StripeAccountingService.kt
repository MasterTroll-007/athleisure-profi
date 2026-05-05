package com.fitness.service

import com.fitness.config.StripeConfig
import com.fitness.entity.StripeBalanceTransaction
import com.fitness.entity.StripePayout
import com.fitness.repository.StripeBalanceTransactionRepository
import com.fitness.repository.StripePaymentRepository
import com.fitness.repository.StripePayoutRepository
import com.stripe.model.BalanceTransaction
import com.stripe.model.Charge
import com.stripe.model.Payout
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant

data class StripeAccountingSyncResult(
    val stripeConfigured: Boolean,
    val balanceTransactionsSynced: Int = 0,
    val payoutsSynced: Int = 0,
    val paymentsEnriched: Int = 0
)

private data class ChargeAccountingData(
    val chargeId: String,
    val paymentIntentId: String?,
    val balanceTransactionId: String?
)

@Service
class StripeAccountingService(
    private val stripeConfig: StripeConfig,
    private val balanceTransactionRepository: StripeBalanceTransactionRepository,
    private val payoutRepository: StripePayoutRepository,
    private val stripePaymentRepository: StripePaymentRepository
) {
    private val logger = LoggerFactory.getLogger(StripeAccountingService::class.java)

    @Transactional
    fun syncPeriod(from: Instant, to: Instant): StripeAccountingSyncResult {
        if (!stripeConfig.isConfigured()) {
            logger.info("Skipping Stripe accounting sync because Stripe is not configured")
            return StripeAccountingSyncResult(stripeConfigured = false)
        }

        var balanceTransactionsSynced = 0
        var payoutsSynced = 0
        var paymentsEnriched = 0

        for (balanceTransaction in listBalanceTransactions(from, to)) {
            val sourceId = balanceTransaction.source?.takeIf { it.isNotBlank() }
            val chargeAccountingData = sourceId
                ?.takeIf { it.startsWith("ch_") }
                ?.let { retrieveChargeAccountingData(it) }

            val saved = upsertBalanceTransaction(balanceTransaction, chargeAccountingData)
            balanceTransactionsSynced += 1

            if (chargeAccountingData != null) {
                if (enrichLocalPayment(saved, chargeAccountingData)) {
                    paymentsEnriched += 1
                }
            }
        }

        for (payout in listPayouts(from, to)) {
            upsertPayout(payout)
            payoutsSynced += 1
        }

        return StripeAccountingSyncResult(
            stripeConfigured = true,
            balanceTransactionsSynced = balanceTransactionsSynced,
            payoutsSynced = payoutsSynced,
            paymentsEnriched = paymentsEnriched
        )
    }

    private fun listBalanceTransactions(from: Instant, to: Instant): List<BalanceTransaction> {
        val transactions = mutableListOf<BalanceTransaction>()
        var startingAfter: String? = null

        do {
            val params = linkedMapOf<String, Any>(
                "limit" to 100L,
                "created" to mapOf(
                    "gte" to from.epochSecond,
                    "lt" to to.epochSecond
                )
            )
            startingAfter?.let { params["starting_after"] = it }

            val page = BalanceTransaction.list(params)
            transactions += page.data
            startingAfter = page.data.lastOrNull()?.id
        } while (page.hasMore)

        return transactions
    }

    private fun listPayouts(from: Instant, to: Instant): List<Payout> {
        val payouts = mutableListOf<Payout>()
        var startingAfter: String? = null

        do {
            val params = linkedMapOf<String, Any>(
                "limit" to 100L,
                "created" to mapOf(
                    "gte" to from.epochSecond,
                    "lt" to to.epochSecond
                )
            )
            startingAfter?.let { params["starting_after"] = it }

            val page = Payout.list(params)
            payouts += page.data
            startingAfter = page.data.lastOrNull()?.id
        } while (page.hasMore)

        return payouts
    }

    private fun retrieveChargeAccountingData(chargeId: String): ChargeAccountingData? {
        return try {
            val charge = Charge.retrieve(chargeId)
            ChargeAccountingData(
                chargeId = charge.id,
                paymentIntentId = charge.paymentIntent,
                balanceTransactionId = charge.balanceTransaction
            )
        } catch (e: Exception) {
            logger.warn("Could not retrieve Stripe charge for accounting sync: chargeId=$chargeId, reason=${e.message}")
            null
        }
    }

    private fun upsertBalanceTransaction(
        balanceTransaction: BalanceTransaction,
        chargeAccountingData: ChargeAccountingData?
    ): StripeBalanceTransaction {
        val existing = balanceTransactionRepository.findByStripeBalanceTransactionId(balanceTransaction.id)
        val sourceId = balanceTransaction.source?.takeIf { it.isNotBlank() }
        val now = Instant.now()

        val entity = StripeBalanceTransaction(
            id = existing?.id,
            stripeBalanceTransactionId = balanceTransaction.id,
            stripeSourceId = sourceId,
            stripeChargeId = chargeAccountingData?.chargeId ?: sourceId?.takeIf { it.startsWith("ch_") },
            stripePaymentIntentId = chargeAccountingData?.paymentIntentId ?: existing?.stripePaymentIntentId,
            stripePayoutId = existing?.stripePayoutId,
            type = balanceTransaction.type,
            reportingCategory = balanceTransaction.reportingCategory,
            description = balanceTransaction.description,
            currency = balanceTransaction.currency.uppercase(),
            amountCents = balanceTransaction.amount,
            feeCents = balanceTransaction.fee,
            netCents = balanceTransaction.net,
            status = balanceTransaction.status,
            createdAtStripe = Instant.ofEpochSecond(balanceTransaction.created),
            availableOn = balanceTransaction.availableOn?.let { Instant.ofEpochSecond(it) },
            syncedAt = now
        )

        return balanceTransactionRepository.save(entity)
    }

    private fun upsertPayout(payout: Payout): StripePayout {
        val existing = payoutRepository.findByStripePayoutId(payout.id)
        val entity = StripePayout(
            id = existing?.id,
            stripePayoutId = payout.id,
            stripeBalanceTransactionId = payout.balanceTransaction,
            amountCents = payout.amount,
            currency = payout.currency.uppercase(),
            status = payout.status,
            createdAtStripe = Instant.ofEpochSecond(payout.created),
            arrivalDate = payout.arrivalDate?.let { Instant.ofEpochSecond(it) },
            method = payout.method,
            type = payout.type,
            description = payout.description,
            statementDescriptor = payout.statementDescriptor,
            failureCode = payout.failureCode,
            failureMessage = payout.failureMessage,
            syncedAt = Instant.now()
        )

        return payoutRepository.save(entity)
    }

    private fun enrichLocalPayment(
        balanceTransaction: StripeBalanceTransaction,
        chargeAccountingData: ChargeAccountingData
    ): Boolean {
        val payment = chargeAccountingData.paymentIntentId
            ?.let { stripePaymentRepository.findByStripePaymentIntentId(it) }
            ?: stripePaymentRepository.findByStripeChargeId(chargeAccountingData.chargeId)
            ?: return false

        stripePaymentRepository.save(
            payment.copy(
                stripePaymentIntentId = chargeAccountingData.paymentIntentId ?: payment.stripePaymentIntentId,
                stripeChargeId = chargeAccountingData.chargeId,
                stripeBalanceTransactionId = balanceTransaction.stripeBalanceTransactionId,
                stripePayoutId = balanceTransaction.stripePayoutId ?: payment.stripePayoutId,
                stripeFeeAmount = centsToDecimal(balanceTransaction.feeCents),
                stripeNetAmount = centsToDecimal(balanceTransaction.netCents),
                updatedAt = Instant.now()
            )
        )
        return true
    }

    private fun centsToDecimal(cents: Long): BigDecimal =
        BigDecimal.valueOf(cents).divide(BigDecimal.valueOf(100), 2, RoundingMode.UNNECESSARY)
}
