package com.fitness.controller

import com.fitness.service.StripeService
import org.slf4j.LoggerFactory
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
        val success = stripeService.handleWebhookEvent(event)

        return if (success) {
            ResponseEntity.ok(mapOf("status" to "ok"))
        } else {
            ResponseEntity.badRequest().body(mapOf("error" to "Event processing failed"))
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
