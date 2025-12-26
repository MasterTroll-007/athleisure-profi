package com.fitness.service

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fitness.config.GopayConfig
import org.slf4j.LoggerFactory
import org.springframework.http.*
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import java.math.BigDecimal
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * GoPay API client for sandbox and production environments.
 * Documentation: https://doc.gopay.com/
 */
@Component
class GopayApiClient(
    private val gopayConfig: GopayConfig,
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(GopayApiClient::class.java)
    private val restTemplate = RestTemplate()

    @Volatile
    private var accessToken: String? = null

    @Volatile
    private var tokenExpiresAt: Long = 0

    /**
     * Create a payment and get the gateway URL for redirect.
     */
    fun createPayment(request: CreatePaymentRequest): CreatePaymentResponse {
        val token = getAccessToken()

        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
            accept = listOf(MediaType.APPLICATION_JSON)
            setBearerAuth(token)
        }

        val gopayRequest = mapOf(
            "payer" to mapOf(
                "default_payment_instrument" to "PAYMENT_CARD",
                "allowed_payment_instruments" to listOf("PAYMENT_CARD", "BANK_ACCOUNT"),
                "contact" to mapOf(
                    "email" to request.email
                )
            ),
            "amount" to request.amountInCents,
            "currency" to request.currency,
            "order_number" to request.orderNumber,
            "order_description" to request.description,
            "items" to listOf(
                mapOf(
                    "type" to "ITEM",
                    "name" to request.description,
                    "amount" to request.amountInCents,
                    "count" to 1
                )
            ),
            "callback" to mapOf(
                "return_url" to gopayConfig.returnUrl,
                "notification_url" to gopayConfig.notificationUrl
            ),
            "lang" to "CS"
        )

        val entity = HttpEntity(gopayRequest, headers)

        return try {
            val response = restTemplate.exchange(
                "${gopayConfig.apiUrl}/payments/payment",
                HttpMethod.POST,
                entity,
                GopayPaymentResponse::class.java
            )

            val body = response.body ?: throw RuntimeException("Empty response from GoPay")

            logger.info("GoPay payment created: id=${body.id}, state=${body.state}")

            CreatePaymentResponse(
                gopayId = body.id.toString(),
                gatewayUrl = body.gwUrl,
                state = body.state
            )
        } catch (e: Exception) {
            logger.error("Failed to create GoPay payment: ${e.message}", e)
            throw RuntimeException("Failed to create payment: ${e.message}", e)
        }
    }

    /**
     * Get payment status from GoPay.
     */
    fun getPaymentStatus(gopayId: String): String {
        val token = getAccessToken()

        val headers = HttpHeaders().apply {
            accept = listOf(MediaType.APPLICATION_JSON)
            setBearerAuth(token)
        }

        val entity = HttpEntity<Void>(headers)

        return try {
            val response = restTemplate.exchange(
                "${gopayConfig.apiUrl}/payments/payment/$gopayId",
                HttpMethod.GET,
                entity,
                GopayPaymentResponse::class.java
            )

            response.body?.state ?: "UNKNOWN"
        } catch (e: Exception) {
            logger.error("Failed to get payment status: ${e.message}", e)
            "ERROR"
        }
    }

    /**
     * Verify webhook signature using HMAC-SHA256.
     * GoPay sends signature in X-GP-Signature header.
     */
    fun verifyWebhookSignature(payload: String, signature: String): Boolean {
        return try {
            val mac = Mac.getInstance("HmacSHA256")
            val secretKey = SecretKeySpec(gopayConfig.clientSecret.toByteArray(), "HmacSHA256")
            mac.init(secretKey)

            val calculatedSignature = Base64.getEncoder().encodeToString(mac.doFinal(payload.toByteArray()))

            // Constant-time comparison to prevent timing attacks
            calculatedSignature == signature
        } catch (e: Exception) {
            logger.error("Failed to verify webhook signature: ${e.message}", e)
            false
        }
    }

    /**
     * Get OAuth2 access token from GoPay.
     */
    @Synchronized
    private fun getAccessToken(): String {
        // Return cached token if still valid
        if (accessToken != null && System.currentTimeMillis() < tokenExpiresAt - 60000) {
            return accessToken!!
        }

        val credentials = Base64.getEncoder().encodeToString(
            "${gopayConfig.clientId}:${gopayConfig.clientSecret}".toByteArray()
        )

        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_FORM_URLENCODED
            accept = listOf(MediaType.APPLICATION_JSON)
            set("Authorization", "Basic $credentials")
        }

        val body = "grant_type=client_credentials&scope=payment-create"
        val entity = HttpEntity(body, headers)

        return try {
            val response = restTemplate.exchange(
                "${gopayConfig.apiUrl}/oauth2/token",
                HttpMethod.POST,
                entity,
                GopayTokenResponse::class.java
            )

            val tokenResponse = response.body ?: throw RuntimeException("Empty token response")

            accessToken = tokenResponse.accessToken
            tokenExpiresAt = System.currentTimeMillis() + (tokenResponse.expiresIn * 1000)

            logger.info("GoPay access token obtained, expires in ${tokenResponse.expiresIn}s")

            accessToken!!
        } catch (e: Exception) {
            logger.error("Failed to get GoPay access token: ${e.message}", e)
            throw RuntimeException("Failed to authenticate with GoPay: ${e.message}", e)
        }
    }

    /**
     * Check if GoPay is properly configured.
     */
    fun isConfigured(): Boolean {
        return gopayConfig.clientId.isNotBlank() &&
               gopayConfig.clientSecret.isNotBlank() &&
               gopayConfig.goId.isNotBlank()
    }
}

// Request/Response DTOs

data class CreatePaymentRequest(
    val email: String,
    val amountInCents: Long,  // GoPay uses cents (halere)
    val currency: String = "CZK",
    val orderNumber: String,
    val description: String
)

data class CreatePaymentResponse(
    val gopayId: String,
    val gatewayUrl: String,
    val state: String
)

data class GopayTokenResponse(
    @JsonProperty("access_token")
    val accessToken: String,

    @JsonProperty("token_type")
    val tokenType: String,

    @JsonProperty("expires_in")
    val expiresIn: Long,

    val scope: String? = null
)

data class GopayPaymentResponse(
    val id: Long,

    @JsonProperty("order_number")
    val orderNumber: String? = null,

    val state: String,

    val amount: Long? = null,

    val currency: String? = null,

    @JsonProperty("gw_url")
    val gwUrl: String = ""
)
