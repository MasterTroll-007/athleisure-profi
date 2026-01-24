package com.fitness.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service

@Service
class EmailService(
    private val mailSender: JavaMailSender,
    @Value("\${app.name}") private val appName: String,
    @Value("\${app.base-url}") private val baseUrl: String,
    @Value("\${spring.mail.username:}") private val fromEmail: String
) {
    private val logger = LoggerFactory.getLogger(EmailService::class.java)

    @Async
    fun sendVerificationEmail(to: String, token: String, firstName: String?) {
        try {
            val verificationUrl = "$baseUrl/verify-email?token=$token"
            val name = firstName ?: "uživateli"

            val htmlContent = """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                        .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                        .header { background: linear-gradient(135deg, #6366f1, #8b5cf6); color: white; padding: 30px; text-align: center; border-radius: 10px 10px 0 0; }
                        .content { background: #f9fafb; padding: 30px; border-radius: 0 0 10px 10px; }
                        .button { display: inline-block; background: #6366f1; color: white; padding: 14px 28px; text-decoration: none; border-radius: 8px; font-weight: bold; margin: 20px 0; }
                        .button:hover { background: #4f46e5; }
                        .footer { text-align: center; margin-top: 20px; color: #6b7280; font-size: 12px; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>$appName</h1>
                        </div>
                        <div class="content">
                            <h2>Ahoj $name!</h2>
                            <p>Děkujeme za registraci. Pro aktivaci účtu prosím klikni na tlačítko níže:</p>
                            <p style="text-align: center;">
                                <a href="$verificationUrl" class="button">Ověřit email</a>
                            </p>
                            <p>Nebo zkopíruj tento odkaz do prohlížeče:</p>
                            <p style="word-break: break-all; color: #6366f1;">$verificationUrl</p>
                            <p>Odkaz je platný 24 hodin.</p>
                            <p>Pokud jsi se neregistroval/a, tento email ignoruj.</p>
                        </div>
                        <div class="footer">
                            <p>$appName &copy; 2024</p>
                        </div>
                    </div>
                </body>
                </html>
            """.trimIndent()

            val message = mailSender.createMimeMessage()
            val helper = MimeMessageHelper(message, true, "UTF-8")

            helper.setFrom(fromEmail, appName)
            helper.setTo(to)
            helper.setSubject("Ověření emailu - $appName")
            helper.setText(htmlContent, true)

            mailSender.send(message)
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

            val htmlContent = """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                        .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                        .header { background: linear-gradient(135deg, #6366f1, #8b5cf6); color: white; padding: 30px; text-align: center; border-radius: 10px 10px 0 0; }
                        .content { background: #f9fafb; padding: 30px; border-radius: 0 0 10px 10px; }
                        .button { display: inline-block; background: #6366f1; color: white; padding: 14px 28px; text-decoration: none; border-radius: 8px; font-weight: bold; margin: 20px 0; }
                        .button:hover { background: #4f46e5; }
                        .footer { text-align: center; margin-top: 20px; color: #6b7280; font-size: 12px; }
                        .warning { background: #fef3c7; border: 1px solid #f59e0b; padding: 12px; border-radius: 8px; margin: 15px 0; font-size: 14px; color: #92400e; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>$appName</h1>
                        </div>
                        <div class="content">
                            <h2>Ahoj $name!</h2>
                            <p>Obdrželi jsme žádost o obnovení hesla k vašemu účtu. Klikněte na tlačítko níže pro nastavení nového hesla:</p>
                            <p style="text-align: center;">
                                <a href="$resetUrl" class="button">Obnovit heslo</a>
                            </p>
                            <p>Nebo zkopíruj tento odkaz do prohlížeče:</p>
                            <p style="word-break: break-all; color: #6366f1;">$resetUrl</p>
                            <div class="warning">
                                ⚠️ Odkaz je platný pouze 1 hodinu. Pokud jste o obnovení hesla nežádali, tento email ignorujte.
                            </div>
                        </div>
                        <div class="footer">
                            <p>$appName &copy; 2024</p>
                        </div>
                    </div>
                </body>
                </html>
            """.trimIndent()

            val message = mailSender.createMimeMessage()
            val helper = MimeMessageHelper(message, true, "UTF-8")

            helper.setFrom(fromEmail, appName)
            helper.setTo(to)
            helper.setSubject("Obnovení hesla - $appName")
            helper.setText(htmlContent, true)

            mailSender.send(message)
            logger.info("Password reset email sent to: $to")
        } catch (e: Exception) {
            logger.error("Failed to send password reset email to: $to", e)
        }
    }
}
