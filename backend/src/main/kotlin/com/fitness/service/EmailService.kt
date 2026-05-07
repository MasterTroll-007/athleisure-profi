package com.fitness.service

import org.slf4j.LoggerFactory
import org.springframework.core.io.ByteArrayResource
import org.springframework.beans.factory.annotation.Value
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalTime
import java.time.Year
import java.time.format.DateTimeFormatter

@Service
class EmailService(
    private val mailSender: JavaMailSender,
    @Value("\${app.name}") private val appName: String,
    @Value("\${app.base-url}") private val baseUrl: String,
    @Value("\${spring.mail.username:}") private val smtpUsername: String,
    @Value("\${app.mail.from-address:}") private val configuredFromEmail: String,
    @Value("\${app.mail.from-name:\${app.name}}") private val fromName: String
) {
    private val logger = LoggerFactory.getLogger(EmailService::class.java)

    private data class ReminderTexts(
        val subject: String,
        val greeting: String,
        val reminderText: String,
        val detailsLabel: String,
        val dateLabel: String,
        val timeLabel: String,
        val noteText: String,
        val footer: String
    )
    private fun htmlEscape(input: String): String = input
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;")

    private val dateFormatterCs = DateTimeFormatter.ofPattern("d. M. yyyy")
    private val dateFormatterEn = DateTimeFormatter.ofPattern("MMMM d, yyyy")
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    private fun fromEmail(): String = configuredFromEmail.ifBlank { smtpUsername }

    private fun baseEmailStyle() = """
        html, body { margin: 0; padding: 0; }
        * { box-sizing: border-box; }
        body {
            width: 100% !important;
            font-family: Outfit, Inter, "Segoe UI", Arial, sans-serif;
            line-height: 1.6;
            color: rgba(255, 255, 255, 0.9);
            background: #05040a;
            -webkit-text-size-adjust: 100%;
            -ms-text-size-adjust: 100%;
        }
        table {
            border-collapse: collapse;
            mso-table-lspace: 0pt;
            mso-table-rspace: 0pt;
        }
        td { vertical-align: top; }
        a { color: #ffcb73; overflow-wrap: anywhere; word-break: break-word; }
        .email-bg {
            width: 100%;
            min-width: 100%;
            background:
                radial-gradient(circle at 50% 0%, rgba(255, 179, 71, 0.16), transparent 34%),
                radial-gradient(circle at 12% 12%, rgba(255, 255, 255, 0.06), transparent 28%),
                linear-gradient(180deg, #07060d 0%, #05040a 48%, #030307 100%);
            padding: 28px 14px;
        }
        .container {
            width: 100%;
            max-width: 640px;
            margin: 0 auto;
        }
        .brand-line {
            color: rgba(255, 255, 255, 0.62);
            font-size: 12px;
            font-weight: 700;
            letter-spacing: 0.08em;
            margin: 0 0 10px;
            text-align: center;
            text-transform: uppercase;
        }
        .shell {
            width: 100%;
            overflow: hidden;
            border: 1px solid rgba(255, 255, 255, 0.1);
            border-radius: 16px;
            background: rgba(9, 8, 16, 0.88);
            box-shadow: 0 28px 76px -44px rgba(0, 0, 0, 0.95);
        }
        .header {
            color: white;
            padding: 30px 28px 24px;
            text-align: left;
            background:
                linear-gradient(180deg, rgba(255, 255, 255, 0.08), transparent 72%),
                linear-gradient(135deg, rgba(255, 220, 139, 0.18), rgba(255, 255, 255, 0.03));
            border-bottom: 1px solid rgba(255, 255, 255, 0.1);
        }
        .accent-bar {
            width: 76px;
            height: 3px;
            border-radius: 999px;
            margin: 0 0 18px;
            box-shadow: 0 0 18px rgba(255, 179, 71, 0.34);
        }
        .eyebrow {
            color: #ffcb73;
            font-size: 12px;
            font-weight: 700;
            letter-spacing: 0.08em;
            margin: 0 0 6px;
            text-transform: uppercase;
        }
        .header h1 {
            color: #ffffff;
            font-size: 26px;
            line-height: 1.18;
            margin: 0;
            font-weight: 800;
        }
        .content {
            padding: 28px;
            background:
                linear-gradient(180deg, rgba(255, 255, 255, 0.035), transparent 48%),
                rgba(9, 8, 16, 0.92);
        }
        .content h2 {
            color: #ffffff;
            font-size: 21px;
            line-height: 1.25;
            margin: 0 0 14px;
        }
        .content h3 {
            color: #ffffff;
            font-size: 17px;
            line-height: 1.3;
        }
        .content p { color: rgba(255, 255, 255, 0.78); margin: 12px 0; }
        .content strong { color: rgba(255, 255, 255, 0.96); }
        .breakable {
            overflow-wrap: anywhere;
            word-break: break-word;
        }
        .button {
            display: inline-block;
            min-width: 150px;
            max-width: 100%;
            color: #17151d !important;
            background:
                linear-gradient(180deg, rgba(255, 255, 255, 0.82) 0%, rgba(255, 255, 255, 0.3) 34%, rgba(255, 255, 255, 0.1) 100%),
                linear-gradient(180deg, #fff4d5 0%, #dcb96e 58%, #efd399 100%);
            border: 1px solid rgba(255, 220, 139, 0.48);
            border-radius: 10px;
            box-shadow:
                inset 0 1px 0 rgba(255, 255, 255, 0.78),
                0 16px 34px -18px rgba(255, 179, 71, 0.62);
            font-weight: 800;
            margin: 20px 0;
            padding: 13px 24px;
            text-align: center;
            text-decoration: none;
            text-shadow: 0 1px 0 rgba(255, 255, 255, 0.32);
            white-space: nowrap;
        }
        .details {
            background: rgba(255, 255, 255, 0.055);
            border: 1px solid rgba(255, 255, 255, 0.1);
            border-radius: 12px;
            margin: 18px 0;
            padding: 18px 20px;
        }
        .details * { overflow-wrap: anywhere; word-break: break-word; }
        .details p { margin: 8px 0; }
        .details-row { display: flex; gap: 12px; margin: 10px 0; }
        .details-label { color: rgba(255, 255, 255, 0.58); font-weight: 800; min-width: 82px; }
        .details-value { color: rgba(255, 255, 255, 0.9); }
        .warning {
            background: rgba(245, 158, 11, 0.13);
            border: 1px solid rgba(245, 158, 11, 0.34);
            border-radius: 12px;
            color: #ffe0a8;
            font-size: 14px;
            margin: 18px 0;
            padding: 14px 16px;
        }
        .footer { color: rgba(255, 255, 255, 0.42); font-size: 12px; margin-top: 16px; text-align: center; }
        @media screen and (max-width: 520px) {
            .email-bg { padding: 14px 8px !important; }
            .container { width: 100% !important; max-width: 100% !important; }
            .brand-line { font-size: 11px !important; margin-bottom: 8px !important; }
            .shell { border-radius: 12px !important; width: 100% !important; }
            .header { padding: 22px 16px 20px !important; }
            .content { padding: 22px 16px !important; }
            .header h1 { font-size: 22px !important; line-height: 1.18 !important; }
            .content h2 { font-size: 19px !important; line-height: 1.28 !important; }
            .content h3 { font-size: 16px !important; }
            .content p { font-size: 15px !important; line-height: 1.55 !important; }
            .accent-bar { margin-bottom: 14px !important; }
            .button {
                display: block !important;
                width: 100% !important;
                min-width: 0 !important;
                padding-left: 14px !important;
                padding-right: 14px !important;
                white-space: normal !important;
            }
            .details { padding: 15px 14px !important; margin: 16px 0 !important; }
            .details-row { display: block !important; }
            .details-label { display: block !important; min-width: 0 !important; margin-bottom: 2px !important; }
        }
    """.trimIndent()

    private fun wrapEmail(headerGradient: String, headerTitle: String, bodyContent: String, extraStyles: String = ""): String {
        val year = Year.now().value
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    ${baseEmailStyle()}
                    $extraStyles
                </style>
            </head>
            <body style="margin:0; padding:0; background:#05040a;">
                <table role="presentation" width="100%" cellpadding="0" cellspacing="0" class="email-bg" style="width:100%; min-width:100%;">
                    <tr>
                        <td align="center">
                            <table role="presentation" width="100%" cellpadding="0" cellspacing="0" class="container" style="width:100%; max-width:640px; margin:0 auto;">
                                <tr>
                                    <td align="center">
                                        <p class="brand-line">$appName</p>
                                    </td>
                                </tr>
                                <tr>
                                    <td>
                                        <table role="presentation" width="100%" cellpadding="0" cellspacing="0" class="shell" style="width:100%;">
                                            <tr>
                                                <td class="header">
                                                    <div class="accent-bar" style="background: linear-gradient(135deg, $headerGradient);"></div>
                                                    <p class="eyebrow">$appName</p>
                                                    <h1>$headerTitle</h1>
                                                </td>
                                            </tr>
                                            <tr>
                                                <td class="content">
                                                    $bodyContent
                                                </td>
                                            </tr>
                                        </table>
                                    </td>
                                </tr>
                                <tr>
                                    <td class="footer">
                                        <p>$appName &copy; $year</p>
                                    </td>
                                </tr>
                            </table>
                        </td>
                    </tr>
                </table>
            </body>
            </html>
        """.trimIndent()
    }

    private fun sendHtmlEmail(to: String, subject: String, htmlContent: String) {
        val message = mailSender.createMimeMessage()
        val helper = MimeMessageHelper(message, true, "UTF-8")
        helper.setFrom(fromEmail(), fromName)
        helper.setTo(to)
        helper.setSubject(subject)
        helper.setText(htmlContent, true)
        mailSender.send(message)
    }

    private fun sendHtmlEmailWithAttachment(
        to: String,
        subject: String,
        htmlContent: String,
        attachmentFilename: String,
        attachmentBytes: ByteArray,
        attachmentContentType: String
    ) {
        val message = mailSender.createMimeMessage()
        val helper = MimeMessageHelper(message, true, "UTF-8")
        helper.setFrom(fromEmail(), fromName)
        helper.setTo(to)
        helper.setSubject(subject)
        helper.setText(htmlContent, true)
        helper.addAttachment(
            attachmentFilename,
            ByteArrayResource(attachmentBytes),
            attachmentContentType
        )
        mailSender.send(message)
    }

    @Async
    fun sendVerificationEmail(to: String, token: String, firstName: String?) {
        try {
            val verificationUrl = "$baseUrl/verify-email?token=$token"
            val name = firstName ?: "uživateli"

            val htmlContent = wrapEmail("#ffdc8b, #f29b2f", appName, """
                <h2>Ahoj $name!</h2>
                <p>Děkujeme za registraci. Pro aktivaci účtu prosím klikni na tlačítko níže:</p>
                <p style="text-align: center;">
                    <a href="$verificationUrl" class="button">Ověřit email</a>
                </p>
                <p>Nebo zkopíruj tento odkaz do prohlížeče:</p>
                <p style="word-break: break-all; color: #ffcb73;">$verificationUrl</p>
                <p>Odkaz je platný 24 hodin.</p>
                <p>Pokud jsi se neregistroval/a, tento email ignoruj.</p>
            """.trimIndent())

            sendHtmlEmail(to, "Ověření emailu - $appName", htmlContent)
            logger.info("Verification email sent to: $to")
        } catch (e: Exception) {
            logger.error("Failed to send verification email to: $to", e)
        }
    }

    @Async
    fun sendPasswordResetEmail(to: String, token: String, firstName: String?) {
        try {
            val resetUrl = "$baseUrl/reset-password?token=$token"
            val name = firstName ?: "uživateli"

            val htmlContent = wrapEmail("#ffdc8b, #f29b2f", appName, """
                <h2>Ahoj $name!</h2>
                <p>Obdrželi jsme žádost o obnovení hesla k vašemu účtu. Klikněte na tlačítko níže pro nastavení nového hesla:</p>
                <p style="text-align: center;">
                    <a href="$resetUrl" class="button">Obnovit heslo</a>
                </p>
                <p>Nebo zkopíruj tento odkaz do prohlížeče:</p>
                <p style="word-break: break-all; color: #ffcb73;">$resetUrl</p>
                <div class="warning">
                    Odkaz je platný pouze 30 minut. Pokud jste o obnovení hesla nežádali, tento email ignorujte.
                </div>
            """.trimIndent())

            sendHtmlEmail(to, "Obnovení hesla - $appName", htmlContent)
            logger.info("Password reset email sent to: $to")
        } catch (e: Exception) {
            logger.error("Failed to send password reset email to: $to", e)
        }
    }

    @Async
    fun sendAdminNewReservationEmail(
        adminEmail: String,
        adminName: String?,
        clientName: String,
        clientEmail: String,
        date: LocalDate,
        startTime: LocalTime,
        endTime: LocalTime
    ) {
        try {
            val formattedDate = date.format(dateFormatterCs)
            val formattedStart = startTime.format(timeFormatter)
            val formattedEnd = endTime.format(timeFormatter)

            val htmlContent = wrapEmail("#10b981, #059669", "Nová rezervace", """
                <h2>Ahoj ${htmlEscape(adminName ?: "trenére")}!</h2>
                <p>Máš novou rezervaci:</p>
                <div class="details" style="border-left: 4px solid #10b981;">
                    <p><strong>Klient:</strong> ${htmlEscape(clientName)} (${htmlEscape(clientEmail)})</p>
                    <p><strong>Datum:</strong> $formattedDate</p>
                    <p><strong>Čas:</strong> $formattedStart - $formattedEnd</p>
                </div>
            """.trimIndent())

            sendHtmlEmail(adminEmail, "Nová rezervace - $clientName - $appName", htmlContent)
            logger.info("Admin notification (new reservation) sent to: $adminEmail")
        } catch (e: Exception) {
            logger.error("Failed to send admin new reservation email to: $adminEmail", e)
        }
    }

    @Async
    fun sendAdminCancelledReservationEmail(
        adminEmail: String,
        adminName: String?,
        clientName: String,
        clientEmail: String,
        date: LocalDate,
        startTime: LocalTime,
        endTime: LocalTime
    ) {
        try {
            val formattedDate = date.format(dateFormatterCs)
            val formattedStart = startTime.format(timeFormatter)
            val formattedEnd = endTime.format(timeFormatter)

            val htmlContent = wrapEmail("#ef4444, #dc2626", "Zrušená rezervace", """
                <h2>Ahoj ${htmlEscape(adminName ?: "trenére")}!</h2>
                <p>Klient zrušil rezervaci:</p>
                <div class="details" style="border-left: 4px solid #ef4444;">
                    <p><strong>Klient:</strong> ${htmlEscape(clientName)} (${htmlEscape(clientEmail)})</p>
                    <p><strong>Datum:</strong> $formattedDate</p>
                    <p><strong>Čas:</strong> $formattedStart - $formattedEnd</p>
                </div>
            """.trimIndent())

            sendHtmlEmail(adminEmail, "Zrušená rezervace - $clientName - $appName", htmlContent)
            logger.info("Admin notification (cancelled reservation) sent to: $adminEmail")
        } catch (e: Exception) {
            logger.error("Failed to send admin cancelled reservation email to: $adminEmail", e)
        }
    }

    @Async
    fun sendCreditExpirationWarning(to: String, firstName: String?, credits: Int, expiresAt: String, daysUntil: Int) {
        try {
            val name = firstName ?: "klientko"
            val urgency = if (daysUntil <= 1) "Zítra" else "Za $daysUntil dní"

            val htmlContent = wrapEmail("#f59e0b, #d97706", "Expirace kreditů", """
                <h2>Ahoj $name!</h2>
                <p>$urgency ti vyprší <strong>$credits kreditů</strong>.</p>
                <div class="details" style="border-left: 4px solid #f59e0b;">
                    <p><strong>Počet kreditů:</strong> $credits</p>
                    <p><strong>Datum expirace:</strong> $expiresAt</p>
                </div>
                <p>Využij je včas a zarezervuj si trénink!</p>
                <p style="text-align: center;">
                    <a href="$baseUrl/calendar" class="button" style="background: #f59e0b;">Zarezervovat trénink</a>
                </p>
            """.trimIndent())

            sendHtmlEmail(to, "$urgency ti vyprší kredity - $appName", htmlContent)
            logger.info("Credit expiration warning sent to: $to ($daysUntil days)")
        } catch (e: Exception) {
            logger.error("Failed to send credit expiration warning to: $to", e)
        }
    }

    @Async
    fun sendSlotCancelledByTrainerEmail(
        to: String,
        firstName: String?,
        date: String,
        time: String,
        creditsRefunded: Int,
        reason: String? = null
    ) {
        try {
            val name = firstName ?: "klientko"
            val reasonHtml = reason
                ?.takeIf { it.isNotBlank() }
                ?.let { "<p><strong>Důvod:</strong> ${htmlEscape(it)}</p>" }
                ?: ""

            val htmlContent = wrapEmail("#ef4444, #dc2626", "Zrušený trénink", """
                <h2>Ahoj $name!</h2>
                <p>Tvůj trénink byl zrušen trenérem:</p>
                <div class="details" style="border-left: 4px solid #ef4444;">
                    <p><strong>Datum:</strong> $date</p>
                    <p><strong>Čas:</strong> $time</p>
                    $reasonHtml
                    <p><strong>Vráceno kreditů:</strong> $creditsRefunded</p>
                </div>
                <p>Kredity byly automaticky vráceny na tvůj účet. Omlouváme se za komplikace.</p>
            """.trimIndent())

            sendHtmlEmail(to, "Zrušený trénink - $appName", htmlContent)
            logger.info("Slot cancelled by trainer email sent to: $to")
        } catch (e: Exception) {
            logger.error("Failed to send slot cancelled email to: $to", e)
        }
    }

    @Async
    fun sendAnnouncementEmail(to: String, firstName: String?, subject: String, message: String, trainerName: String) {
        try {
            val name = htmlEscape(firstName ?: "klientko")
            val safeSubject = htmlEscape(subject)
            val safeMessage = htmlEscape(message).replace("\n", "<br>")
            val safeTrainer = htmlEscape(trainerName)
            val emailSubject = subject.replace(Regex("[\\r\\n]"), " ")

            val htmlContent = wrapEmail("#ffdc8b, #f29b2f", "Zpráva od trenéra", """
                <h2>Ahoj $name!</h2>
                <p>Tvůj trenér <strong>$safeTrainer</strong> ti posílá zprávu:</p>
                <div class="details" style="border-left: 4px solid #ffb347;">
                    <h3 style="margin-top: 0;">$safeSubject</h3>
                    <p>$safeMessage</p>
                </div>
            """.trimIndent())

            sendHtmlEmail(to, "$emailSubject - $appName", htmlContent)
            logger.info("Announcement email sent to: $to")
        } catch (e: Exception) {
            logger.error("Failed to send announcement email to: $to", e)
        }
    }

    @Async
    fun sendMonthlyReportEmail(to: String, trainerName: String?, stats: Map<String, Any>) {
        try {
            val name = trainerName ?: "trenére"

            val htmlContent = wrapEmail("#ffdc8b, #f29b2f", "Měsíční report", """
                <h2>Ahoj $name!</h2>
                <p>Zde je tvůj měsíční přehled:</p>
                <div class="details" style="border-left: 4px solid #ffb347;">
                    <p><strong>Dokončené tréninky:</strong> ${stats["completedSessions"]}</p>
                    <p><strong>Noví klienti:</strong> ${stats["newClients"]}</p>
                    <p><strong>Prodané kredity:</strong> ${stats["creditsSold"]}</p>
                    <p><strong>Attendance rate:</strong> ${stats["attendanceRate"]}%</p>
                    <p><strong>No-show rate:</strong> ${stats["noShowRate"]}%</p>
                </div>
            """.trimIndent())

            sendHtmlEmail(to, "Měsíční report - $appName", htmlContent)
            logger.info("Monthly report sent to: $to")
        } catch (e: Exception) {
            logger.error("Failed to send monthly report to: $to", e)
        }
    }

    @Async
    fun sendMonthlyAccountingReportEmail(
        to: String,
        trainerName: String?,
        periodLabel: String,
        stats: Map<String, Any>,
        accountingSummary: AccountingExportSummary,
        attachmentFilename: String,
        attachmentBytes: ByteArray
    ) {
        try {
            val name = trainerName ?: "trenére"

            val htmlContent = wrapEmail("#ffdc8b, #f29b2f", "Měsíční účetní report", """
                <h2>Ahoj ${htmlEscape(name)}!</h2>
                <p>V příloze je účetní ZIP export za období <strong>${htmlEscape(periodLabel)}</strong>.</p>
                <div class="details" style="border-left: 4px solid #ffb347;">
                    <p><strong>Zaplacené platby:</strong> ${accountingSummary.completedPaymentsCount}</p>
                    <p><strong>Hrubé tržby:</strong> ${accountingSummary.grossPaid.setScale(2)} CZK</p>
                    <p><strong>Stripe poplatky:</strong> ${accountingSummary.stripeFees.setScale(2)} CZK</p>
                    <p><strong>Čistě po poplatcích:</strong> ${accountingSummary.netAfterFees.setScale(2)} CZK</p>
                    <p><strong>Prodané kredity:</strong> ${accountingSummary.creditsSold}</p>
                    <p><strong>Výplaty ze Stripe:</strong> ${accountingSummary.payoutsCount} / ${accountingSummary.payoutsTotal.setScale(2)} CZK</p>
                </div>
                <div class="details" style="border-left: 4px solid #10b981;">
                    <p><strong>Dokončené tréninky:</strong> ${stats["completedSessions"]}</p>
                    <p><strong>Noví klienti:</strong> ${stats["newClients"]}</p>
                    <p><strong>Docházka:</strong> ${stats["attendanceRate"]}%</p>
                    <p><strong>No-show:</strong> ${stats["noShowRate"]}%</p>
                </div>
            """.trimIndent())

            sendHtmlEmailWithAttachment(
                to = to,
                subject = "Měsíční účetní report $periodLabel - $appName",
                htmlContent = htmlContent,
                attachmentFilename = attachmentFilename,
                attachmentBytes = attachmentBytes,
                attachmentContentType = "application/zip"
            )
            logger.info("Monthly accounting report sent to: $to")
        } catch (e: Exception) {
            logger.error("Failed to send monthly accounting report to: $to", e)
        }
    }

    @Async
    fun sendReminderEmail(
        to: String,
        firstName: String?,
        date: LocalDate,
        startTime: LocalTime,
        endTime: LocalTime,
        locale: String = "cs"
    ) {
        try {
            val name = firstName ?: if (locale == "cs") "klientko" else "client"
            val formattedDate = if (locale == "cs") date.format(dateFormatterCs) else date.format(dateFormatterEn)
            val formattedStartTime = startTime.format(timeFormatter)
            val formattedEndTime = endTime.format(timeFormatter)

            val (subject, greeting, reminderText, detailsLabel, dateLabel, timeLabel, noteText, _) = if (locale == "cs") {
                ReminderTexts(
                    subject = "Připomínka tréninku - $appName",
                    greeting = "Ahoj $name!",
                    reminderText = "Připomínáme ti nadcházející trénink:",
                    detailsLabel = "Detaily tréninku",
                    dateLabel = "Datum",
                    timeLabel = "Čas",
                    noteText = "Těšíme se na tebe!",
                    footer = "$appName © ${Year.now().value}"
                )
            } else {
                ReminderTexts(
                    subject = "Training Reminder - $appName",
                    greeting = "Hi $name!",
                    reminderText = "This is a reminder for your upcoming training session:",
                    detailsLabel = "Training Details",
                    dateLabel = "Date",
                    timeLabel = "Time",
                    noteText = "We look forward to seeing you!",
                    footer = "$appName © ${Year.now().value}"
                )
            }

            val htmlContent = wrapEmail("#ffdc8b, #f29b2f", appName, """
                <h2>$greeting</h2>
                <p>$reminderText</p>
                <div class="details" style="border-left: 4px solid #ffb347;">
                    <h3 style="margin-top: 0; color: #ffcb73;">$detailsLabel</h3>
                    <div class="details-row">
                        <span class="details-label">$dateLabel:</span>
                        <span class="details-value">$formattedDate</span>
                    </div>
                    <div class="details-row">
                        <span class="details-label">$timeLabel:</span>
                        <span class="details-value">$formattedStartTime - $formattedEndTime</span>
                    </div>
                </div>
                <p>$noteText</p>
            """.trimIndent())

            sendHtmlEmail(to, subject, htmlContent)
            logger.info("Reminder email sent to: $to for reservation on $date at $formattedStartTime")
        } catch (e: Exception) {
            logger.error("Failed to send reminder email to: $to", e)
        }
    }
}
