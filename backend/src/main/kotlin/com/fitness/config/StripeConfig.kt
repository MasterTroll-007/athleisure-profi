package com.fitness.config

import com.stripe.Stripe
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "stripe")
class StripeConfig {
    private val logger = LoggerFactory.getLogger(StripeConfig::class.java)

    var secretKey: String = ""
    var publishableKey: String = ""
    var webhookSecret: String = ""
    var successUrl: String = ""
    var cancelUrl: String = ""

    @PostConstruct
    fun init() {
        if (secretKey.isNotBlank()) {
            Stripe.apiKey = secretKey
            logger.info("Stripe initialized with API key: ${secretKey.take(12)}...")
        } else {
            logger.warn("Stripe secret key not configured - payments will use simulation mode")
        }
    }

    fun isConfigured(): Boolean = secretKey.isNotBlank()
}
