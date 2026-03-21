package com.fitness.service

import com.fitness.repository.CreditTransactionRepository
import com.fitness.repository.UserRepository
import com.fitness.entity.displayName
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

@Service
class ReceiptService(
    private val creditTransactionRepository: CreditTransactionRepository,
    private val userRepository: UserRepository,
    @Value("\${app.name}") private val appName: String
) {
    private val dateFormatter = DateTimeFormatter.ofPattern("d. M. yyyy HH:mm")

    fun generateReceipt(transactionId: String, userId: String): String {
        val txUUID = UUID.fromString(transactionId)
        val userUUID = UUID.fromString(userId)

        val transaction = creditTransactionRepository.findById(txUUID)
            .orElseThrow { NoSuchElementException("Transaction not found") }

        if (transaction.userId != userUUID) {
            throw IllegalArgumentException("Access denied")
        }

        if (transaction.type != "purchase") {
            throw IllegalArgumentException("Receipts are only available for purchase transactions")
        }

        val user = userRepository.findById(userUUID).orElse(null)
        val userName = user?.displayName ?: "Unknown"
        val userEmail = user?.email ?: ""
        val date = transaction.createdAt.atZone(ZoneId.of("Europe/Prague")).format(dateFormatter)

        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <title>Potvrzení platby - $appName</title>
                <style>
                    @media print { body { margin: 0; } .no-print { display: none; } }
                    body { font-family: Arial, sans-serif; max-width: 600px; margin: 40px auto; padding: 20px; color: #333; }
                    .header { text-align: center; border-bottom: 2px solid #6366f1; padding-bottom: 20px; margin-bottom: 20px; }
                    .header h1 { color: #6366f1; margin: 0; }
                    .header p { color: #6b7280; margin: 5px 0; }
                    .details { margin: 20px 0; }
                    .details table { width: 100%; border-collapse: collapse; }
                    .details td { padding: 8px 0; border-bottom: 1px solid #e5e7eb; }
                    .details td:first-child { font-weight: bold; color: #6b7280; width: 40%; }
                    .total { background: #f3f4f6; padding: 15px; border-radius: 8px; margin: 20px 0; text-align: center; }
                    .total .amount { font-size: 24px; font-weight: bold; color: #6366f1; }
                    .footer { text-align: center; color: #9ca3af; font-size: 12px; margin-top: 30px; border-top: 1px solid #e5e7eb; padding-top: 15px; }
                    .print-btn { display: block; margin: 20px auto; padding: 10px 30px; background: #6366f1; color: white; border: none; border-radius: 8px; cursor: pointer; font-size: 16px; }
                    .print-btn:hover { background: #4f46e5; }
                </style>
            </head>
            <body>
                <div class="header">
                    <h1>$appName</h1>
                    <p>Potvrzení o nákupu kreditů</p>
                </div>
                <div class="details">
                    <table>
                        <tr><td>ID transakce</td><td>${transaction.id}</td></tr>
                        <tr><td>Datum</td><td>$date</td></tr>
                        <tr><td>Klient</td><td>$userName</td></tr>
                        <tr><td>Email</td><td>$userEmail</td></tr>
                        <tr><td>Počet kreditů</td><td>${transaction.amount}</td></tr>
                        <tr><td>Poznámka</td><td>${transaction.note ?: "-"}</td></tr>
                    </table>
                </div>
                <div class="total">
                    <div>Zakoupeno kreditů</div>
                    <div class="amount">${transaction.amount}</div>
                </div>
                <button class="print-btn no-print" onclick="window.print()">Vytisknout / Uložit PDF</button>
                <div class="footer">
                    <p>Tento doklad byl vygenerován automaticky systémem $appName.</p>
                    <p>Datum vygenerování: ${java.time.LocalDateTime.now().atZone(ZoneId.of("Europe/Prague")).format(dateFormatter)}</p>
                </div>
            </body>
            </html>
        """.trimIndent()
    }
}
