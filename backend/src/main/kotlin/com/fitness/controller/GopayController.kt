package com.fitness.controller

import com.fitness.service.GopayService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/gopay")
class GopayController(
    private val gopayService: GopayService
) {

    @PostMapping("/webhook")
    fun handleWebhook(@RequestBody payload: GopayWebhookPayload): ResponseEntity<Map<String, String>> {
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
