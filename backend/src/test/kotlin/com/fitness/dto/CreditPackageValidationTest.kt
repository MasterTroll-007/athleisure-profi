package com.fitness.dto

import jakarta.validation.Validation
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class CreditPackageValidationTest {
    private val validator = Validation.buildDefaultValidatorFactory().validator

    @Test
    fun `create credit package rejects prices below Stripe CZK minimum`() {
        val violations = validator.validate(
            CreateCreditPackageRequest(
                nameCs = "Test",
                credits = 1,
                priceCzk = BigDecimal("14.99")
            )
        )

        assertThat(violations.map { it.propertyPath.toString() }).contains("priceCzk")
    }

    @Test
    fun `update credit package rejects prices below Stripe CZK minimum`() {
        val violations = validator.validate(
            UpdateCreditPackageRequest(
                priceCzk = BigDecimal("1.00")
            )
        )

        assertThat(violations.map { it.propertyPath.toString() }).contains("priceCzk")
    }

    @Test
    fun `credit package accepts Stripe CZK minimum price`() {
        val violations = validator.validate(
            CreateCreditPackageRequest(
                nameCs = "Valid",
                credits = 1,
                priceCzk = BigDecimal("15.00")
            )
        )

        assertThat(violations).isEmpty()
    }
}
