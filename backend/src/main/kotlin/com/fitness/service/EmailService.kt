package com.fitness.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Service
class EmailService(
    private val mailSender: JavaMailSender,
    @Value("\${app.name}") private val appName: String,
    @Value("\${app.base-url}") private val baseUrl: String,
    @Value("\${spring.mail.username:}") private val fromEmail: String
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
    private val dateFormatterCs = DateTimeFormatter.ofPattern("d. M. yyyy")
    private val dateFormatterEn = DateTimeFormatter.ofPattern("MMMM d, yyyy")
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    private fun baseEmailStyle() = """
        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
        .container { max-width: 600px; margin: 0 auto; padding: 20px; }
        .header { color: white; padding: 30px; text-align: center; border-radius: 10px 10px 0 0; }
        .content { background: #f9fafb; padding: 30px; border-radius: 0 0 10px 10px; }
        .button { display: inline-block; background: #6366f1; color: white; padding: 14px 28px; text-decoration: none; border-radius: 8px; font-weight: bold; margin: 20px 0; }
        .button:hover { background: #4f46e5; }
        .details { background: white; padding: 20px; border-radius: 8px; margin: 20px 0; }
        .footer { text-align: center; margin-top: 20px; color: #6b7280; font-size: 12px; }
        .warning { background: #fef3c7; border: 1px solid #f59e0b; padding: 12px; border-radius: 8px; margin: 15px 0; font-size: 14px; color: #92400e; }
    """.trimIndent()

    private fun wrapEmail(headerGradient: String, headerTitle: String, bodyContent: String, extraStyles: String = ""): String {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    ${baseEmailStyle()}
                    $extraStyles
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header" style="background: linear-gradient(135deg, $headerGradient);">
                        <h1>$headerTitle</h1>
                    </div>
                    <div class="content">
                        $bodyContent
                    </div>
                    <div class="footer">
                        <p>$appName &copy; 2024</p>
                    </div>
                </div>
            </body>
            </html>
        """.trimIndent()
    }

    private fun sendHtmlEmail(to: String, subject: String, htmlContent: String) {
        val message = mailSender.createMimeMessage()
        val helper = MimeMessageHelper(message, true, "UTF-8")
        helper.setFrom(fromEmail, appName)
        helper.setTo(to)
        helper.setSubject(subject)
        helper.setText(htmlContent, true)
        mailSender.send(message)
    }

    @Async
    fun sendVerificationEmail(to: String, token: String, firstName: String?) {
        try {
            val verificationUrl = "$baseUrl/verify-email?token=$token"
            val name = firstName ?: "uživateli"

            val htmlContent = wrapEmail("#6366f1, #8b5cf6", appName, """
                <h2>Ahoj $name!</h2>
                <p>Děkujeme za registraci. Pro aktivaci účtu prosím klikni na tlačítko níže:</p>
                <p style="text-align: center;">
                    <a href="$verificationUrl" class="button">Ověřit email</a>
                </p>
                <p>Nebo zkopíruj tento odkaz do prohlížeče:</p>
                <p style="word-break: break-all; color: #6366f1;">$verificationUrl</p>
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

            val htmlContent = wrapEmail("#6366f1, #8b5cf6", appName, """
                <h2>Ahoj $name!</h2>
                <p>Obdrželi jsme žádost o obnovení hesla k vašemu účtu. Klikněte na tlačítko níže pro nastavení nového hesla:</p>
                <p style="text-align: center;">
                    <a href="$resetUrl" class="button">Obnovit heslo</a>
                </p>
                <p>Nebo zkopíruj tento odkaz do prohlížeče:</p>
                <p style="word-break: break-all; color: #6366f1;">$resetUrl</p>
                <div class="warning">
                    ⚠️ Odkaz je platný pouze 30 minut. Pokud jste o obnovení hesla nežádali, tento email ignorujte.
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
                <h2>Ahoj ${adminName ?: "trenére"}!</h2>
                <p>Máš novou rezervaci:</p>
                <div class="details" style="border-left: 4px solid #10b981;">
                    <p><strong>Klient:</strong> $clientName ($clientEmail)</p>
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
                <h2>Ahoj ${adminName ?: "trenére"}!</h2>
                <p>Klient zrušil rezervaci:</p>
                <div class="details" style="border-left: 4px solid #ef4444;">
                    <p><strong>Klient:</strong> $clientName ($clientEmail)</p>
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
                    footer = "$appName © 2024"
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
                    footer = "$appName © 2024"
                )
            }

            val extraStyles = """
                .details-row { display: flex; margin: 10px 0; }
                .details-label { font-weight: bold; color: #6b7280; width: 80px; }
                .details-value { color: #1f2937; }
            """.trimIndent()

            val htmlContent = wrapEmail("#6366f1, #8b5cf6", appName, """
                <h2>$greeting</h2>
                <p>$reminderText</p>
                <div class="details" style="border-left: 4px solid #6366f1;">
                    <h3 style="margin-top: 0; color: #6366f1;">$detailsLabel</h3>
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
            """.trimIndent(), extraStyles)

            sendHtmlEmail(to, subject, htmlContent)
            logger.info("Reminder email sent to: $to for reservation on $date at $formattedStartTime")
        } catch (e: Exception) {
            logger.error("Failed to send reminder email to: $to", e)
        }
    }
}
