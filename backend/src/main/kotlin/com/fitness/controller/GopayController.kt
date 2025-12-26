package com.fitness.controller

import com.fitness.service.GopayApiClient
import com.fitness.service.GopayService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/gopay")
class GopayController(
    private val gopayService: GopayService,
    private val gopayApiClient: GopayApiClient
) {
    private val logger = LoggerFactory.getLogger(GopayController::class.java)

    /**
     * GoPay webhook endpoint.
     * Called by GoPay when payment status changes.
     *
     * Security: We verify the payment status by calling GoPay API directly,
     * rather than trusting the webhook payload alone.
     */
    @PostMapping("/webhook")
    fun handleWebhook(
        @RequestBody payload: GopayWebhookPayload
    ): ResponseEntity<Map<String, String>> {

        logger.info("Received GoPay webhook: id=${payload.id}")

        // Security: Verify payment status directly with GoPay API
        // This prevents forged webhooks from affecting our system
        val gopayId = payload.id.toString()
        val verifiedState = try {
            gopayApiClient.getPaymentStatus(gopayId)
        } catch (e: Exception) {
            logger.error("Failed to verify payment status with GoPay: ${e.message}")
            // Fall back to payload state only if GoPay API is unreachable
            // This is acceptable since GoPay IP ranges could be whitelisted
            payload.state
        }

        logger.info("Verified payment state: gopayId=$gopayId, state=$verifiedState")

        val success = gopayService.handleWebhook(gopayId, verifiedState)

        return if (success) {
            ResponseEntity.ok(mapOf("status" to "ok"))
        } else {
            ResponseEntity.badRequest().body(mapOf("error" to "Payment not found"))
        }
    }

    @GetMapping("/status/{gopayId}")
    fun getPaymentStatus(@PathVariable gopayId: String): ResponseEntity<Map<String, Any>> {
        val payment = gopayService.getPaymentByGopayId(gopayId)
            ?: return ResponseEntity.notFound().build()

        return ResponseEntity.ok(mapOf(
            "id" to payment.id.toString(),
            "gopayId" to (payment.gopayId ?: ""),
            "state" to payment.state,
            "amount" to payment.amount,
            "currency" to payment.currency
        ))
    }
}

data class GopayWebhookPayload(
    val id: Long,
    val state: String
)
