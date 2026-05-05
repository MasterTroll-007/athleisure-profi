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
    private val displayDateFormatter = DateTimeFormatter.ofPattern("d. M. yyyy")
    private val displayDateTimeFormatter = DateTimeFormatter.ofPattern("d. M. yyyy HH:mm")

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

    fun buildPeriodPackage(trainerId: UUID, startDate: LocalDate, endDate: LocalDate): AccountingExportPackage {
        require(!endDate.isBefore(startDate)) { "End date must be the same as or after start date" }

        return buildPackage(
            trainerId = trainerId,
            from = startDate.atStartOfDay(zone).toInstant(),
            to = endDate.plusDays(1).atStartOfDay(zone).toInstant(),
            periodStart = startDate,
            periodEnd = endDate,
            filename = "accounting-${startDate}_${endDate}.zip"
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
        val stripeFees = completedPayments.sumOfMoney { payment -> paymentFee(payment, balanceById) ?: BigDecimal.ZERO }
        val netAfterFees = completedPayments.sumOfMoney { payment -> paymentNet(payment, balanceById) ?: payment.amount }
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
            htmlFile(
                "souhrn.html",
                htmlReport(
                    summary = summary,
                    payments = payments,
                    creditTransactions = creditTransactions,
                    payouts = payouts,
                    users = users,
                    packages = packages,
                    balanceTransactions = balanceById,
                    generatedAt = Instant.now()
                )
            ),
            csvFile("summary.csv", summaryRows(summary)),
            csvFile("payments.csv", paymentRows(payments, users, packages, balanceById)),
            csvFile("balance_transactions.csv", balanceTransactionRows(balanceTransactions)),
            csvFile("payouts.csv", payoutRows(payouts)),
            csvFile("credit_movements.csv", creditMovementRows(creditTransactions, users))
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

    private data class ZipFile(val filename: String, val bytes: ByteArray)

    private fun csvFile(filename: String, rows: List<List<String>>) =
        ZipFile(filename, csvExportService.buildCsv(rows))

    private fun htmlFile(filename: String, html: String) =
        ZipFile(filename, html.toByteArray(Charsets.UTF_8))

    private fun buildZip(vararg files: ZipFile): ByteArray {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            files.forEach { file ->
                zip.putNextEntry(ZipEntry(file.filename))
                zip.write(file.bytes)
                zip.closeEntry()
            }
        }
        return output.toByteArray()
    }

    private fun htmlReport(
        summary: AccountingExportSummary,
        payments: List<StripePayment>,
        creditTransactions: List<com.fitness.entity.CreditTransaction>,
        payouts: List<StripePayout>,
        users: Map<UUID?, User>,
        packages: Map<UUID?, CreditPackage>,
        balanceTransactions: Map<String, StripeBalanceTransaction>,
        generatedAt: Instant
    ): String {
        val completedPayments = payments.filter { it.status.equals("completed", ignoreCase = true) }
        val pendingStripeBalance = summary.netAfterFees - summary.payoutsTotal
        val missingFeeCount = completedPayments.count { payment ->
            payment.stripeFeeAmount == null &&
                payment.stripeBalanceTransactionId?.let { balanceTransactions[it] } == null
        }
        val statusRows = payments
            .groupBy { it.status.ifBlank { "unknown" } }
            .toSortedMap()
            .map { (status, items) ->
                listOf(
                    html(status),
                    items.size.toString(),
                    htmlMoney(items.sumOfMoney { it.amount })
                )
            }
        val dailyRows = completedPayments
            .groupBy { LocalDate.ofInstant(it.createdAt, zone) }
            .toSortedMap()
            .map { (date, items) ->
                listOf(
                    displayDate(date),
                    items.size.toString(),
                    htmlMoney(items.sumOfMoney { it.amount }),
                    htmlMoney(items.sumOfMoney { paymentFee(it, balanceTransactions) ?: BigDecimal.ZERO }),
                    htmlMoney(items.sumOfMoney { paymentNet(it, balanceTransactions) ?: it.amount }),
                    items.sumOf { packages[it.creditPackageId]?.credits ?: 0 }.toString()
                )
            }
        val packageRows = completedPayments
            .groupBy { it.creditPackageId }
            .map { (packageId, items) ->
                val creditPackage = packages[packageId]
                listOf(
                    html(creditPackage?.nameCs ?: "Neznámý balíček"),
                    items.size.toString(),
                    items.sumOf { creditPackage?.credits ?: 0 }.toString(),
                    htmlMoney(items.sumOfMoney { it.amount }),
                    htmlMoney(items.sumOfMoney { paymentFee(it, balanceTransactions) ?: BigDecimal.ZERO }),
                    htmlMoney(items.sumOfMoney { paymentNet(it, balanceTransactions) ?: it.amount })
                )
            }
            .sortedBy { it.first() }
        val paymentRows = payments
            .sortedBy { it.createdAt }
            .map { payment ->
                val user = users[payment.userId]
                val creditPackage = packages[payment.creditPackageId]
                listOf(
                    displayDateTime(payment.createdAt),
                    html(clientName(user).ifBlank { user?.email ?: "Neznámý klient" }),
                    html(user?.email.orEmpty()),
                    html(creditPackage?.nameCs.orEmpty()),
                    html(creditPackage?.credits?.toString().orEmpty()),
                    html(payment.status),
                    htmlMoney(payment.amount),
                    paymentFee(payment, balanceTransactions)?.let { htmlMoney(it) } ?: "-",
                    paymentNet(payment, balanceTransactions)?.let { htmlMoney(it) } ?: "-",
                    html(payment.stripeSessionId)
                )
            }
        val payoutRows = payouts
            .sortedBy { it.createdAtStripe }
            .map { payout ->
                listOf(
                    displayDateTime(payout.createdAtStripe),
                    payout.arrivalDate?.let { displayDateTime(it) } ?: "-",
                    html(payout.status),
                    htmlMoney(centsToDecimal(payout.amountCents)),
                    html(payout.stripePayoutId)
                )
            }
        val creditRows = creditTransactions
            .sortedBy { it.createdAt }
            .map { transaction ->
                val user = users[transaction.userId]
                listOf(
                    displayDateTime(transaction.createdAt),
                    html(clientName(user).ifBlank { user?.email ?: "Neznámý klient" }),
                    html(transaction.type),
                    transaction.amount.toString(),
                    html(transaction.note.orEmpty()),
                    html(transaction.stripePaymentId.orEmpty())
                )
            }
        val creditTypeRows = creditTransactions
            .groupBy { it.type }
            .toSortedMap()
            .map { (type, items) ->
                listOf(html(type), items.size.toString(), items.sumOf { it.amount }.toString())
            }

        return """
            <!doctype html>
            <html lang="cs">
            <head>
              <meta charset="utf-8">
              <meta name="viewport" content="width=device-width, initial-scale=1">
              <title>Účetní souhrn ${displayDate(summary.periodStart)} - ${displayDate(summary.periodEnd)}</title>
              <style>
                :root {
                  color-scheme: light;
                  --ink: #162033;
                  --muted: #647084;
                  --line: #d9e0ea;
                  --soft: #f3f6fa;
                  --panel: #ffffff;
                  --accent: #8b5e34;
                  --accent-strong: #4d351f;
                  --good: #0f766e;
                  --warn: #b45309;
                }
                * { box-sizing: border-box; }
                body {
                  margin: 0;
                  background: #edf1f6;
                  color: var(--ink);
                  font-family: Inter, ui-sans-serif, system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
                  line-height: 1.45;
                }
                .page {
                  max-width: 1180px;
                  margin: 0 auto;
                  padding: 32px 24px 48px;
                }
                .hero {
                  display: grid;
                  gap: 18px;
                  grid-template-columns: minmax(0, 1fr) auto;
                  align-items: start;
                  padding: 28px;
                  border: 1px solid var(--line);
                  border-radius: 18px;
                  background: linear-gradient(135deg, #fff 0%, #f7f1eb 100%);
                  box-shadow: 0 18px 42px rgba(22, 32, 51, 0.10);
                }
                .eyebrow {
                  margin: 0 0 8px;
                  color: var(--accent);
                  font-size: 12px;
                  font-weight: 800;
                  letter-spacing: .12em;
                  text-transform: uppercase;
                }
                h1 {
                  margin: 0;
                  font-size: 30px;
                  line-height: 1.12;
                }
                h2 {
                  margin: 0 0 14px;
                  font-size: 18px;
                }
                h3 {
                  margin: 0 0 8px;
                  font-size: 14px;
                  color: var(--muted);
                  text-transform: uppercase;
                  letter-spacing: .08em;
                }
                .meta {
                  min-width: 230px;
                  padding: 14px 16px;
                  border: 1px solid rgba(139, 94, 52, .24);
                  border-radius: 14px;
                  background: rgba(255, 255, 255, .72);
                  font-size: 13px;
                  color: var(--muted);
                }
                .meta strong { display: block; color: var(--ink); font-size: 15px; }
                .grid {
                  display: grid;
                  gap: 14px;
                }
                .summary-grid {
                  grid-template-columns: repeat(4, minmax(0, 1fr));
                  margin-top: 18px;
                }
                .card, .section {
                  border: 1px solid var(--line);
                  border-radius: 16px;
                  background: var(--panel);
                  box-shadow: 0 10px 26px rgba(22, 32, 51, 0.07);
                }
                .card {
                  padding: 16px;
                }
                .card .label {
                  color: var(--muted);
                  font-size: 12px;
                  font-weight: 700;
                  text-transform: uppercase;
                  letter-spacing: .06em;
                }
                .card .value {
                  margin-top: 7px;
                  font-size: 24px;
                  font-weight: 850;
                  letter-spacing: -.01em;
                }
                .card .hint {
                  margin-top: 5px;
                  color: var(--muted);
                  font-size: 12px;
                }
                .section {
                  margin-top: 18px;
                  padding: 20px;
                  overflow: hidden;
                }
                .tax-note {
                  display: grid;
                  gap: 10px;
                  grid-template-columns: repeat(2, minmax(0, 1fr));
                  margin-top: 12px;
                }
                .note-item {
                  padding: 12px 14px;
                  border-radius: 12px;
                  background: var(--soft);
                  border: 1px solid #e5ebf2;
                }
                .note-item strong {
                  display: block;
                  margin-bottom: 4px;
                }
                .muted { color: var(--muted); }
                .warn { color: var(--warn); font-weight: 750; }
                .good { color: var(--good); font-weight: 750; }
                table {
                  width: 100%;
                  border-collapse: collapse;
                  font-size: 12px;
                }
                th {
                  background: #f7f9fc;
                  color: #4b5565;
                  font-size: 11px;
                  letter-spacing: .06em;
                  text-align: left;
                  text-transform: uppercase;
                }
                th, td {
                  padding: 10px 9px;
                  border-bottom: 1px solid #e8edf3;
                  vertical-align: top;
                }
                tr:last-child td { border-bottom: 0; }
                td.num, th.num {
                  text-align: right;
                  white-space: nowrap;
                  font-variant-numeric: tabular-nums;
                }
                .table-wrap {
                  overflow-x: auto;
                  border: 1px solid #e8edf3;
                  border-radius: 12px;
                }
                .pill {
                  display: inline-block;
                  padding: 3px 8px;
                  border-radius: 999px;
                  background: #eef2f7;
                  color: #334155;
                  font-size: 11px;
                  font-weight: 750;
                }
                .empty {
                  padding: 18px;
                  color: var(--muted);
                  text-align: center;
                }
                .footer {
                  margin-top: 22px;
                  color: var(--muted);
                  font-size: 12px;
                }
                @media (max-width: 900px) {
                  .hero { grid-template-columns: 1fr; }
                  .summary-grid, .tax-note { grid-template-columns: 1fr 1fr; }
                }
                @media (max-width: 620px) {
                  .page { padding: 18px 12px 32px; }
                  .summary-grid, .tax-note { grid-template-columns: 1fr; }
                  h1 { font-size: 24px; }
                }
                @media print {
                  body { background: #fff; }
                  .page { max-width: none; padding: 0; }
                  .hero, .card, .section { box-shadow: none; break-inside: avoid; }
                  .section { page-break-inside: avoid; }
                  .table-wrap { overflow: visible; }
                }
              </style>
            </head>
            <body>
              <main class="page">
                <section class="hero">
                  <div>
                    <p class="eyebrow">Podklad pro účetnictví</p>
                    <h1>Účetní souhrn za období ${displayDate(summary.periodStart)} - ${displayDate(summary.periodEnd)}</h1>
                    <p class="muted">Přehled plateb přes Stripe, poplatků, čistých částek, výplat a pohybů kreditů. CSV soubory v tomto ZIPu obsahují detailní data pro import nebo kontrolu.</p>
                  </div>
                  <div class="meta">
                    <span>Vygenerováno</span>
                    <strong>${displayDateTime(generatedAt)}</strong>
                    <span>Casova zona: Europe/Prague</span>
                  </div>
                </section>

                <section class="grid summary-grid">
                  ${summaryCard("Hrubé tržby", htmlMoney(summary.grossPaid), "${summary.completedPaymentsCount} uhrazených plateb")}
                  ${summaryCard("Stripe poplatky", htmlMoney(summary.stripeFees), "Náklad platební brány")}
                  ${summaryCard("Čisté po poplatcích", htmlMoney(summary.netAfterFees), "Hrubé tržby minus Stripe poplatky")}
                  ${summaryCard("Vyplaceno na účet", htmlMoney(summary.payoutsTotal), "${summary.payoutsCount} Stripe výplat")}
                  ${summaryCard("Rozdíl u Stripe", htmlMoney(pendingStripeBalance), "Časový posun mezi platbou a výplatou")}
                  ${summaryCard("Prodané kredity", summary.creditsSold.toString(), "Kredity z nákupů v období")}
                  ${summaryCard("Chybějící poplatky", missingFeeCount.toString(), "Platby bez spárovaného Stripe fee")}
                  ${summaryCard("Počet pohybů kreditů", creditTransactions.size.toString(), "Nákupy, čerpání, storna a ruční úpravy")}
                </section>

                <section class="section">
                  <h2>Jak to číst pro účetnictví</h2>
                  <div class="tax-note">
                    <div class="note-item">
                      <strong>Hrubé tržby</strong>
                      Částka, kterou klienti zaplatili před Stripe poplatkem. To je hlavní částka pro kontrolu příjmů.
                    </div>
                    <div class="note-item">
                      <strong>Stripe poplatky</strong>
                      Poplatky platební brány. Obvykle se předávají účetní jako samostatný náklad.
                    </div>
                    <div class="note-item">
                      <strong>Výplaty na účet</strong>
                      Peníze odeslané ze Stripe na bankovní účet. Nejsou to další tržby, ale převod už přijatých plateb.
                    </div>
                    <div class="note-item">
                      <strong>Kontrola rozdílu</strong>
                      Rozdíl mezi čistou částkou a výplatami může vznikat časovým posunem Stripe výplat nebo refundacemi mimo vybrané období.
                    </div>
                  </div>
                  <p class="footer">Tento report je praktický podklad. Finální způsob zaúčtování a daňové klasifikace potvrdí účetní podle aktuální evidence podnikání.</p>
                </section>

                ${tableSection("Stavy plateb", listOf("Stav", "Počet", "Hrubá částka"), statusRows, setOf(1, 2), "Žádné platby v období.")}
                ${tableSection("Platby po dnech", listOf("Datum", "Počet", "Hrubé tržby", "Poplatky", "Čisté", "Kredity"), dailyRows, setOf(1, 2, 3, 4, 5), "Žádné uhrazené platby v období.")}
                ${tableSection("Prodej podle balíčku", listOf("Balíček", "Počet", "Kredity", "Hrubé tržby", "Poplatky", "Čisté"), packageRows, setOf(1, 2, 3, 4, 5), "Žádné prodané balíčky v období.")}
                ${tableSection("Detail plateb", listOf("Datum", "Klient", "E-mail", "Balíček", "Kredity", "Stav", "Hrubé", "Poplatek", "Čisté", "Stripe session"), paymentRows, setOf(4, 6, 7, 8), "Žádné platby v období.")}
                ${tableSection("Výplaty Stripe", listOf("Vytvořeno", "Doražení", "Stav", "Částka", "Stripe payout"), payoutRows, setOf(3), "Žádné výplaty v období.")}
                ${tableSection("Souhrn pohybu kreditů", listOf("Typ", "Počet", "Součet kreditů"), creditTypeRows, setOf(1, 2), "Žádné pohyby kreditů v období.")}
                ${tableSection("Detail pohybu kreditů", listOf("Datum", "Klient", "Typ", "Kredity", "Poznámka", "Stripe payment"), creditRows, setOf(3), "Žádné pohyby kreditů v období.")}

                <p class="footer">Detailní CSV data jsou ve stejném ZIPu: summary.csv, payments.csv, balance_transactions.csv, payouts.csv a credit_movements.csv.</p>
              </main>
            </body>
            </html>
        """.trimIndent()
    }

    private fun summaryCard(label: String, value: String, hint: String): String =
        """
            <article class="card">
              <div class="label">${html(label)}</div>
              <div class="value">$value</div>
              <div class="hint">${html(hint)}</div>
            </article>
        """.trimIndent()

    private fun tableSection(
        title: String,
        headers: List<String>,
        rows: List<List<String>>,
        numericColumns: Set<Int>,
        emptyMessage: String
    ): String {
        val headerHtml = headers.mapIndexed { index, header ->
            val className = if (index in numericColumns) " class=\"num\"" else ""
            "<th$className>${html(header)}</th>"
        }.joinToString("")
        val bodyHtml = if (rows.isEmpty()) {
            """<tr><td class="empty" colspan="${headers.size}">${html(emptyMessage)}</td></tr>"""
        } else {
            rows.joinToString("") { row ->
                val cells = headers.indices.joinToString("") { index ->
                    val className = if (index in numericColumns) " class=\"num\"" else ""
                    val value = row.getOrNull(index).orEmpty()
                    "<td$className>$value</td>"
                }
                "<tr>$cells</tr>"
            }
        }

        return """
            <section class="section">
              <h2>${html(title)}</h2>
              <div class="table-wrap">
                <table>
                  <thead><tr>$headerHtml</tr></thead>
                  <tbody>$bodyHtml</tbody>
                </table>
              </div>
            </section>
        """.trimIndent()
    }

    private fun paymentFee(
        payment: StripePayment,
        balanceTransactions: Map<String, StripeBalanceTransaction>
    ): BigDecimal? =
        payment.stripeFeeAmount ?: payment.stripeBalanceTransactionId
            ?.let { balanceTransactions[it] }
            ?.let { centsToDecimal(it.feeCents) }

    private fun paymentNet(
        payment: StripePayment,
        balanceTransactions: Map<String, StripeBalanceTransaction>
    ): BigDecimal? =
        payment.stripeNetAmount ?: payment.stripeBalanceTransactionId
            ?.let { balanceTransactions[it] }
            ?.let { centsToDecimal(it.netCents) }

    private fun centsToDecimal(cents: Long): BigDecimal =
        BigDecimal.valueOf(cents).divide(BigDecimal.valueOf(100), 2, RoundingMode.UNNECESSARY)

    private fun money(value: BigDecimal): String =
        value.setScale(2, RoundingMode.HALF_UP).toPlainString()

    private fun clientName(user: User?): String =
        listOfNotNull(user?.firstName, user?.lastName)
            .joinToString(" ")
            .trim()

    private fun htmlMoney(value: BigDecimal): String = "${html(money(value))}&nbsp;Kč"

    private fun displayDate(date: LocalDate): String =
        date.format(displayDateFormatter)

    private fun displayDateTime(instant: Instant): String =
        instant.atZone(zone).format(displayDateTimeFormatter)

    private fun html(value: String?): String =
        value.orEmpty()
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")

    private fun <T> Iterable<T>.sumOfMoney(selector: (T) -> BigDecimal): BigDecimal =
        fold(BigDecimal.ZERO) { acc, item -> acc + selector(item) }
}
