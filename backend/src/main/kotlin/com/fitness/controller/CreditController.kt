package com.fitness.controller

import com.fitness.dto.*
import com.fitness.security.UserPrincipal
import com.fitness.service.CreditService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/credits")
class CreditController(
    private val creditService: CreditService
) {

    @GetMapping("/balance")
    fun getBalance(@AuthenticationPrincipal principal: UserPrincipal): ResponseEntity<CreditBalanceResponse> {
        val balance = creditService.getBalance(principal.userId)
        return ResponseEntity.ok(balance)
    }

    @GetMapping("/packages")
    fun getPackages(): ResponseEntity<List<CreditPackageDTO>> {
        val packages = creditService.getPackages()
        return ResponseEntity.ok(packages)
    }

    @GetMapping("/pricing")
    fun getPricingItems(): ResponseEntity<List<PricingItemDTO>> {
        val items = creditService.getPricingItems()
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
}
