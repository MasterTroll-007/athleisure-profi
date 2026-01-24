package com.fitness.controller

import com.fitness.service.StripeService
import com.fitness.service.WebhookResult
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/stripe")
class StripeController(
    private val stripeService: StripeService
) {
    private val logger = LoggerFactory.getLogger(StripeController::class.java)

    /**
     * Stripe webhook endpoint.
     * Called by Stripe when payment events occur.
     *
     * The raw request body is needed for signature verification.
     *
     * Response strategy:
     * - 200 OK: Event processed successfully or permanently failed (don't retry)
     * - 400 Bad Request: Invalid signature
     * - 500 Internal Server Error: Transient failure (Stripe should retry)
     */
    @PostMapping("/webhook")
    fun handleWebhook(
        @RequestBody payload: String,
        @RequestHeader("Stripe-Signature") signature: String
    ): ResponseEntity<Map<String, String>> {

        logger.info("Received Stripe webhook")

        // Verify webhook signature
        val event = stripeService.verifyWebhookSignature(payload, signature)
            ?: return ResponseEntity.badRequest().body(mapOf("error" to "Invalid signature"))

        // Process the event
        return when (val result = stripeService.handleWebhookEvent(event)) {
            is WebhookResult.Success -> {
                ResponseEntity.ok(mapOf("status" to "ok"))
            }
            is WebhookResult.PermanentFailure -> {
                // Acknowledge receipt but log the failure for investigation
                logger.warn("Webhook event permanently failed: eventId=${event.id}, reason=${result.reason}")
                ResponseEntity.ok(mapOf("status" to "acknowledged", "warning" to result.reason))
            }
            is WebhookResult.TransientFailure -> {
                // Return 500 so Stripe retries the webhook
                logger.error("Webhook event transiently failed: eventId=${event.id}, reason=${result.reason}")
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(mapOf("error" to result.reason))
            }
        }
    }

    /**
     * Get payment status by session ID.
     */
    @GetMapping("/status/{sessionId}")
    fun getPaymentStatus(@PathVariable sessionId: String): ResponseEntity<Map<String, Any>> {
        val payment = stripeService.getPaymentBySessionId(sessionId)
            ?: return ResponseEntity.notFound().build()

        return ResponseEntity.ok(
            mapOf(
                "sessionId" to payment.stripeSessionId,
                "status" to payment.status,
                "amount" to payment.amount,
                "currency" to payment.currency,
                "createdAt" to payment.createdAt.toString()
            )
        )
    }
}
