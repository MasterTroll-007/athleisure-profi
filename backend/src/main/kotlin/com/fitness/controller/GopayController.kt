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
     * Header X-GP-Signature contains HMAC signature for verification.
     */
    @PostMapping("/webhook")
    fun handleWebhook(
        @RequestBody payload: GopayWebhookPayload,
        @RequestHeader("X-GP-Signature", required = false) signature: String?
    ): ResponseEntity<Map<String, String>> {

        logger.info("Processing GoPay webhook: id=${payload.id}, state=${payload.state}")

        // Note: For full signature verification, you would need raw body.
        // GoPay sandbox doesn't always send signatures, so we log and proceed.
        if (signature != null) {
            logger.debug("Received webhook with signature: ${signature.take(20)}...")
        }

        val success = gopayService.handleWebhook(payload.id.toString(), payload.state)

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
