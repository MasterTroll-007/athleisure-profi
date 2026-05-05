package com.fitness.service

import com.fitness.entity.CreditPackage
import com.fitness.entity.CreditTransaction
import com.fitness.entity.StripeBalanceTransaction
import com.fitness.entity.StripePayment
import com.fitness.entity.StripePayout
import com.fitness.entity.TransactionType
import com.fitness.entity.User
import com.fitness.repository.CreditPackageRepository
import com.fitness.repository.CreditTransactionRepository
import com.fitness.repository.StripeBalanceTransactionRepository
import com.fitness.repository.StripePaymentRepository
import com.fitness.repository.StripePayoutRepository
import com.fitness.repository.UserRepository
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.math.BigDecimal
import java.time.Instant
import java.time.YearMonth
import java.util.UUID
import java.util.zip.ZipInputStream

class AccountingExportServiceTest {
    private val stripePaymentRepository = mockk<StripePaymentRepository>()
    private val balanceTransactionRepository = mockk<StripeBalanceTransactionRepository>()
    private val payoutRepository = mockk<StripePayoutRepository>()
    private val creditTransactionRepository = mockk<CreditTransactionRepository>()
    private val userRepository = mockk<UserRepository>()
    private val creditPackageRepository = mockk<CreditPackageRepository>()

    private val service = AccountingExportService(
        stripePaymentRepository = stripePaymentRepository,
        balanceTransactionRepository = balanceTransactionRepository,
        payoutRepository = payoutRepository,
        creditTransactionRepository = creditTransactionRepository,
        userRepository = userRepository,
        creditPackageRepository = creditPackageRepository,
        csvExportService = CsvExportService()
    )

    @Test
    fun `monthly accounting package contains reconciled Stripe accounting files`() {
        val trainerId = UUID.randomUUID()
        val clientId = UUID.randomUUID()
        val packageId = UUID.randomUUID()
        val paymentId = UUID.randomUUID()
        val month = YearMonth.of(2026, 5)
        val payment = StripePayment(
            id = paymentId,
            userId = clientId,
            stripeSessionId = "cs_live_123",
            stripePaymentIntentId = "pi_123",
            stripeChargeId = "ch_123",
            stripeBalanceTransactionId = "txn_123",
            amount = BigDecimal("500.00"),
            stripeFeeAmount = BigDecimal("12.34"),
            stripeNetAmount = BigDecimal("487.66"),
            currency = "CZK",
            status = "completed",
            creditPackageId = packageId,
            createdAt = Instant.parse("2026-05-05T10:00:00Z"),
            updatedAt = Instant.parse("2026-05-05T10:05:00Z")
        )
        val balanceTransaction = StripeBalanceTransaction(
            stripeBalanceTransactionId = "txn_123",
            stripeSourceId = "ch_123",
            stripeChargeId = "ch_123",
            stripePaymentIntentId = "pi_123",
            type = "charge",
            reportingCategory = "charge",
            currency = "CZK",
            amountCents = 50000,
            feeCents = 1234,
            netCents = 48766,
            status = "available",
            createdAtStripe = Instant.parse("2026-05-05T10:01:00Z")
        )
        val payout = StripePayout(
            stripePayoutId = "po_123",
            stripeBalanceTransactionId = "txn_payout_123",
            amountCents = 48766,
            currency = "CZK",
            status = "paid",
            createdAtStripe = Instant.parse("2026-05-07T10:00:00Z"),
            arrivalDate = Instant.parse("2026-05-08T00:00:00Z")
        )
        val creditTransaction = CreditTransaction(
            id = UUID.randomUUID(),
            userId = clientId,
            amount = 10,
            type = TransactionType.PURCHASE.value,
            stripePaymentId = "cs_live_123",
            note = "Nákup: Kreditový balíček",
            createdAt = Instant.parse("2026-05-05T10:06:00Z")
        )
        val client = User(
            id = clientId,
            email = "client@test.com",
            passwordHash = "hash",
            firstName = "Jana",
            lastName = "Testova",
            trainerId = trainerId
        )
        val creditPackage = CreditPackage(
            id = packageId,
            trainerId = trainerId,
            nameCs = "Kreditový balíček",
            credits = 10,
            priceCzk = BigDecimal("500.00")
        )

        every { stripePaymentRepository.findByTrainerIdAndCreatedAtBetween(trainerId, any(), any()) } returns listOf(payment)
        every { creditTransactionRepository.findByTrainerIdAndCreatedAtBetween(trainerId, any(), any()) } returns listOf(creditTransaction)
        every { payoutRepository.findByCreatedAtStripeBetween(any(), any()) } returns listOf(payout)
        every { balanceTransactionRepository.findByIdsForExport(listOf("txn_123")) } returns listOf(balanceTransaction)
        every { userRepository.findAllById(any<Iterable<UUID>>()) } returns listOf(client)
        every { creditPackageRepository.findAllById(any<Iterable<UUID>>()) } returns listOf(creditPackage)

        val report = service.buildMonthlyPackage(trainerId, month)

        assertThat(report.filename).isEqualTo("accounting-2026-05.zip")
        assertThat(report.summary.grossPaid).isEqualByComparingTo("500.00")
        assertThat(report.summary.stripeFees).isEqualByComparingTo("12.34")
        assertThat(report.summary.netAfterFees).isEqualByComparingTo("487.66")
        assertThat(report.summary.creditsSold).isEqualTo(10)

        val summaryCsv = zipEntry(report.bytes, "summary.csv")
        val paymentsCsv = zipEntry(report.bytes, "payments.csv")
        val payoutsCsv = zipEntry(report.bytes, "payouts.csv")
        val creditMovementsCsv = zipEntry(report.bytes, "credit_movements.csv")

        assertThat(summaryCsv).contains("gross_paid,500.00,CZK", "stripe_fees,12.34,CZK")
        assertThat(paymentsCsv).contains("client@test.com", "Kreditový balíček", "cs_live_123", "txn_123")
        assertThat(payoutsCsv).contains("po_123", "487.66", "paid")
        assertThat(creditMovementsCsv).contains("purchase", "10", "cs_live_123")
    }

    private fun zipEntry(bytes: ByteArray, name: String): String {
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                if (entry.name == name) {
                    return zip.readBytes().toString(Charsets.UTF_8)
                }
            }
        }
        error("ZIP entry not found: $name")
    }
}
