package com.fitness.controller

import com.fitness.dto.*
import com.fitness.security.UserPrincipal
import com.fitness.service.CreditService
import com.fitness.service.ReceiptService
import com.fitness.service.StripeService
import com.fitness.util.pageRequest
import jakarta.validation.Valid
import org.springframework.data.domain.Sort
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/credits")
class CreditController(
    private val creditService: CreditService,
    private val stripeService: StripeService,
    private val receiptService: ReceiptService
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
    fun getTransactions(
        @AuthenticationPrincipal principal: UserPrincipal,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<PageDTO<CreditTransactionDTO>> {
        val transactions = creditService.getTransactionsPage(
            principal.userId,
            pageRequest(page, size, Sort.by("createdAt").descending())
        )
        return ResponseEntity.ok(transactions)
    }

    @GetMapping("/transactions/{id}/receipt")
    fun getReceipt(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable id: String
    ): ResponseEntity<Any> {
        return try {
            val html = receiptService.generateReceipt(id, principal.userId)
            ResponseEntity.ok()
                .header("Content-Type", "text/html; charset=UTF-8")
                .body(html)
        } catch (e: NoSuchElementException) {
            ResponseEntity.status(404).body(mapOf("error" to e.message))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(400).body(mapOf("error" to e.message))
        }
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
        if (stripeService.isConfigured() || !stripeService.isSimulationEnabled()) {
            throw IllegalStateException("Payment simulation is disabled")
        }

        val result = creditService.simulatePaymentSuccess(id, principal.userId)
        return ResponseEntity.ok(result)
    }
}
