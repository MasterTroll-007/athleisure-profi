package com.fitness.controller

import com.fitness.dto.*
import com.fitness.security.UserPrincipal
import com.fitness.service.CreditService
import com.fitness.service.StripeService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/credits")
class CreditController(
    private val creditService: CreditService,
    private val stripeService: StripeService
) {

    @GetMapping("/balance")
    fun getBalance(@AuthenticationPrincipal principal: UserPrincipal): ResponseEntity<CreditBalanceResponse> {
        val balance = creditService.getBalance(principal.userId)
        return ResponseEntity.ok(balance)
    }

    @GetMapping("/packages")
    fun getPackages(@AuthenticationPrincipal principal: UserPrincipal): ResponseEntity<List<CreditPackageDTO>> {
        val packages = creditService.getPackages(principal.userId)
        return ResponseEntity.ok(packages)
    }

    @GetMapping("/pricing")
    fun getPricingItems(@AuthenticationPrincipal principal: UserPrincipal): ResponseEntity<List<PricingItemDTO>> {
        val items = creditService.getPricingItems(principal.userId)
        return ResponseEntity.ok(items)
    }

    @GetMapping(value = ["/transactions", "/history"])
    fun getTransactions(@AuthenticationPrincipal principal: UserPrincipal): ResponseEntity<List<CreditTransactionDTO>> {
        val transactions = creditService.getTransactions(principal.userId)
        return ResponseEntity.ok(transactions)
    }

    @PostMapping("/purchase")
    fun purchaseCredits(
        @AuthenticationPrincipal principal: UserPrincipal,
        @Valid @RequestBody request: PurchaseCreditsRequest
    ): ResponseEntity<PurchaseCreditsResponse> {
        val response = creditService.purchaseCredits(principal.userId, request.packageId)
        return ResponseEntity.ok(response)
    }

    @PostMapping("/payment/{id}/simulate-success")
    fun simulatePaymentSuccess(
        @PathVariable id: String,
        @AuthenticationPrincipal principal: UserPrincipal
    ): ResponseEntity<Map<String, Any>> {
        if (stripeService.isConfigured()) {
            throw IllegalStateException("Payment simulation is only available when Stripe is not configured")
        }

        val result = creditService.simulatePaymentSuccess(id, principal.userId)
        return ResponseEntity.ok(result)
    }
}
