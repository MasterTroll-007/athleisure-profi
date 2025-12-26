package com.fitness.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "gopay")
class GopayConfig {
    var clientId: String = ""
    var clientSecret: String = ""
    var goId: String = ""
    var isProduction: Boolean = false
    var returnUrl: String = ""
    var notificationUrl: String = ""

    val apiUrl: String
        get() = if (isProduction) "https://gate.gopay.cz/api" else "https://gw.sandbox.gopay.com/api"

    val gatewayUrl: String
        get() = if (isProduction) "https://gate.gopay.cz/gp-gw/pay-gw" else "https://gw.sandbox.gopay.com/gp-gw/pay-gw"
}
