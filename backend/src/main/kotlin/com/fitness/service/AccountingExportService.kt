package com.fitness.service

import com.fitness.entity.CreditPackage
import com.fitness.entity.StripeBalanceTransaction
import com.fitness.entity.StripePayment
import com.fitness.entity.StripePayout
import com.fitness.entity.User
import com.fitness.repository.CreditPackageRepository
import com.fitness.repository.CreditTransactionRepository
import com.fitness.repository.StripeBalanceTransactionRepository
import com.fitness.repository.StripePaymentRepository
import com.fitness.repository.StripePayoutRepository
import com.fitness.repository.UserRepository
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

data class AccountingExportPackage(
    val filename: String,
    val bytes: ByteArray,
    val summary: AccountingExportSummary
)

data class AccountingExportSummary(
    val periodStart: LocalDate,
    val periodEnd: LocalDate,
    val completedPaymentsCount: Int,
    val grossPaid: BigDecimal,
    val stripeFees: BigDecimal,
    val netAfterFees: BigDecimal,
    val creditsSold: Int,
    val payoutsCount: Int,
    val payoutsTotal: BigDecimal
)

@Service
class AccountingExportService(
    private val stripePaymentRepository: StripePaymentRepository,
    private val balanceTransactionRepository: StripeBalanceTransactionRepository,
    private val payoutRepository: StripePayoutRepository,
    private val creditTransactionRepository: CreditTransactionRepository,
    private val userRepository: UserRepository,
    private val creditPackageRepository: CreditPackageRepository,
    private val csvExportService: CsvExportService
) {
    private val zone = ZoneId.of("Europe/Prague")
    private val filenameDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM")

    fun buildMonthlyPackage(trainerId: UUID, month: YearMonth): AccountingExportPackage {
        val fromDate = month.atDay(1)
        val toDateExclusive = month.plusMonths(1).atDay(1)
        return buildPackage(
            trainerId = trainerId,
            from = fromDate.atStartOfDay(zone).toInstant(),
            to = toDateExclusive.atStartOfDay(zone).toInstant(),
            periodStart = fromDate,
            periodEnd = toDateExclusive.minusDays(1),
            filename = "accounting-${month.format(filenameDateFormatter)}.zip"
        )
    }

    fun buildPackage(
        trainerId: UUID,
        from: Instant,
        to: Instant,
        periodStart: LocalDate,
        periodEnd: LocalDate,
        filename: String
    ): AccountingExportPackage {
        val payments = stripePaymentRepository.findByTrainerIdAndCreatedAtBetween(trainerId, from, to)
        val creditTransactions = creditTransactionRepository.findByTrainerIdAndCreatedAtBetween(trainerId, from, to)
        val payouts = payoutRepository.findByCreatedAtStripeBetween(from, to)

        val balanceTransactionIds = payments.mapNotNull { it.stripeBalanceTransactionId }.distinct()
        val balanceTransactions = if (balanceTransactionIds.isEmpty()) {
            emptyList()
        } else {
            balanceTransactionRepository.findByIdsForExport(balanceTransactionIds)
        }

        val users = loadUsers(payments, creditTransactions.map { it.userId })
        val packages = loadPackages(payments)
        val balanceById = balanceTransactions.associateBy { it.stripeBalanceTransactionId }

        val completedPayments = payments.filter { it.status.equals("completed", ignoreCase = true) }
        val grossPaid = completedPayments.sumOfMoney { it.amount }
        val stripeFees = completedPayments.sumOfMoney { payment ->
            payment.stripeFeeAmount ?: payment.stripeBalanceTransactionId
                ?.let { balanceById[it] }
                ?.let { centsToDecimal(it.feeCents) }
                ?: BigDecimal.ZERO
        }
        val netAfterFees = completedPayments.sumOfMoney { payment ->
            payment.stripeNetAmount ?: payment.stripeBalanceTransactionId
                ?.let { balanceById[it] }
                ?.let { centsToDecimal(it.netCents) }
                ?: payment.amount
        }
        val creditsSold = creditTransactions
            .filter { it.type == "purchase" }
            .sumOf { it.amount }
        val payoutsTotal = payouts.sumOfMoney { centsToDecimal(it.amountCents) }

        val summary = AccountingExportSummary(
            periodStart = periodStart,
            periodEnd = periodEnd,
            completedPaymentsCount = completedPayments.size,
            grossPaid = grossPaid,
            stripeFees = stripeFees,
            netAfterFees = netAfterFees,
            creditsSold = creditsSold,
            payoutsCount = payouts.size,
            payoutsTotal = payoutsTotal
        )

        val zipBytes = buildZip(
            "summary.csv" to summaryRows(summary),
            "payments.csv" to paymentRows(payments, users, packages, balanceById),
            "balance_transactions.csv" to balanceTransactionRows(balanceTransactions),
            "payouts.csv" to payoutRows(payouts),
            "credit_movements.csv" to creditMovementRows(creditTransactions, users)
        )

        return AccountingExportPackage(filename = filename, bytes = zipBytes, summary = summary)
    }

    private fun loadUsers(payments: List<StripePayment>, transactionUserIds: List<UUID>): Map<UUID?, User> {
        val userIds = (payments.mapNotNull { it.userId } + transactionUserIds).distinct()
        return if (userIds.isEmpty()) emptyMap() else userRepository.findAllById(userIds).associateBy { it.id }
    }

    private fun loadPackages(payments: List<StripePayment>): Map<UUID?, CreditPackage> {
        val packageIds = payments.mapNotNull { it.creditPackageId }.distinct()
        return if (packageIds.isEmpty()) emptyMap() else creditPackageRepository.findAllById(packageIds).associateBy { it.id }
    }

    private fun summaryRows(summary: AccountingExportSummary): List<List<String>> = listOf(
        listOf("metric", "value", "currency"),
        listOf("period_start", summary.periodStart.toString(), ""),
        listOf("period_end", summary.periodEnd.toString(), ""),
        listOf("completed_payments_count", summary.completedPaymentsCount.toString(), ""),
        listOf("gross_paid", money(summary.grossPaid), "CZK"),
        listOf("stripe_fees", money(summary.stripeFees), "CZK"),
        listOf("net_after_fees", money(summary.netAfterFees), "CZK"),
        listOf("credits_sold", summary.creditsSold.toString(), ""),
        listOf("payouts_count", summary.payoutsCount.toString(), ""),
        listOf("payouts_total", money(summary.payoutsTotal), "CZK")
    )

    private fun paymentRows(
        payments: List<StripePayment>,
        users: Map<UUID?, User>,
        packages: Map<UUID?, CreditPackage>,
        balanceTransactions: Map<String, StripeBalanceTransaction>
    ): List<List<String>> {
        val header = listOf(
            "local_payment_id",
            "client_id",
            "client_email",
            "client_name",
            "package_id",
            "package_name",
            "credits",
            "status",
            "gross_amount",
            "stripe_fee",
            "net_amount",
            "currency",
            "stripe_session_id",
            "stripe_payment_intent_id",
            "stripe_charge_id",
            "stripe_balance_transaction_id",
            "stripe_payout_id",
            "created_at",
            "updated_at"
        )

        return listOf(header) + payments.map { payment ->
            val user = users[payment.userId]
            val creditPackage = packages[payment.creditPackageId]
            val balanceTransaction = payment.stripeBalanceTransactionId?.let { balanceTransactions[it] }
            val fee = payment.stripeFeeAmount ?: balanceTransaction?.let { centsToDecimal(it.feeCents) }
            val net = payment.stripeNetAmount ?: balanceTransaction?.let { centsToDecimal(it.netCents) }

            listOf(
                payment.id.toString(),
                payment.userId?.toString().orEmpty(),
                user?.email.orEmpty(),
                clientName(user),
                payment.creditPackageId?.toString().orEmpty(),
                creditPackage?.nameCs.orEmpty(),
                creditPackage?.credits?.toString().orEmpty(),
                payment.status,
                money(payment.amount),
                fee?.let { money(it) }.orEmpty(),
                net?.let { money(it) }.orEmpty(),
                payment.currency,
                payment.stripeSessionId,
                payment.stripePaymentIntentId.orEmpty(),
                payment.stripeChargeId.orEmpty(),
                payment.stripeBalanceTransactionId.orEmpty(),
                payment.stripePayoutId.orEmpty(),
                payment.createdAt.toString(),
                payment.updatedAt.toString()
            )
        }
    }

    private fun balanceTransactionRows(transactions: List<StripeBalanceTransaction>): List<List<String>> {
        val header = listOf(
            "stripe_balance_transaction_id",
            "source_id",
            "charge_id",
            "payment_intent_id",
            "payout_id",
            "type",
            "reporting_category",
            "status",
            "amount",
            "fee",
            "net",
            "currency",
            "created_at_stripe",
            "available_on",
            "description",
            "synced_at"
        )

        return listOf(header) + transactions.map {
            listOf(
                it.stripeBalanceTransactionId,
                it.stripeSourceId.orEmpty(),
                it.stripeChargeId.orEmpty(),
                it.stripePaymentIntentId.orEmpty(),
                it.stripePayoutId.orEmpty(),
                it.type,
                it.reportingCategory.orEmpty(),
                it.status.orEmpty(),
                money(centsToDecimal(it.amountCents)),
                money(centsToDecimal(it.feeCents)),
                money(centsToDecimal(it.netCents)),
                it.currency,
                it.createdAtStripe.toString(),
                it.availableOn?.toString().orEmpty(),
                it.description.orEmpty(),
                it.syncedAt.toString()
            )
        }
    }

    private fun payoutRows(payouts: List<StripePayout>): List<List<String>> {
        val header = listOf(
            "stripe_payout_id",
            "stripe_balance_transaction_id",
            "amount",
            "currency",
            "status",
            "created_at_stripe",
            "arrival_date",
            "method",
            "type",
            "description",
            "statement_descriptor",
            "failure_code",
            "failure_message",
            "synced_at"
        )

        return listOf(header) + payouts.map {
            listOf(
                it.stripePayoutId,
                it.stripeBalanceTransactionId.orEmpty(),
                money(centsToDecimal(it.amountCents)),
                it.currency,
                it.status,
                it.createdAtStripe.toString(),
                it.arrivalDate?.toString().orEmpty(),
                it.method.orEmpty(),
                it.type.orEmpty(),
                it.description.orEmpty(),
                it.statementDescriptor.orEmpty(),
                it.failureCode.orEmpty(),
                it.failureMessage.orEmpty(),
                it.syncedAt.toString()
            )
        }
    }

    private fun creditMovementRows(
        transactions: List<com.fitness.entity.CreditTransaction>,
        users: Map<UUID?, User>
    ): List<List<String>> {
        val header = listOf(
            "transaction_id",
            "client_id",
            "client_email",
            "client_name",
            "type",
            "amount_credits",
            "reference_id",
            "stripe_payment_id",
            "gopay_payment_id",
            "note",
            "expires_at",
            "created_at"
        )

        return listOf(header) + transactions.map { transaction ->
            val user = users[transaction.userId]
            listOf(
                transaction.id.toString(),
                transaction.userId.toString(),
                user?.email.orEmpty(),
                clientName(user),
                transaction.type,
                transaction.amount.toString(),
                transaction.referenceId?.toString().orEmpty(),
                transaction.stripePaymentId.orEmpty(),
                transaction.gopayPaymentId.orEmpty(),
                transaction.note.orEmpty(),
                transaction.expiresAt?.toString().orEmpty(),
                transaction.createdAt.toString()
            )
        }
    }

    private fun buildZip(vararg files: Pair<String, List<List<String>>>): ByteArray {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            files.forEach { (filename, rows) ->
                zip.putNextEntry(ZipEntry(filename))
                zip.write(csvExportService.buildCsv(rows))
                zip.closeEntry()
            }
        }
        return output.toByteArray()
    }

    private fun centsToDecimal(cents: Long): BigDecimal =
        BigDecimal.valueOf(cents).divide(BigDecimal.valueOf(100), 2, RoundingMode.UNNECESSARY)

    private fun money(value: BigDecimal): String =
        value.setScale(2, RoundingMode.HALF_UP).toPlainString()

    private fun clientName(user: User?): String =
        listOfNotNull(user?.firstName, user?.lastName)
            .joinToString(" ")
            .trim()

    private fun <T> Iterable<T>.sumOfMoney(selector: (T) -> BigDecimal): BigDecimal =
        fold(BigDecimal.ZERO) { acc, item -> acc + selector(item) }
}
