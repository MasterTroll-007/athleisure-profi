package com.fitness.service

import com.fitness.config.StripeConfig
import com.fitness.entity.CreditTransaction
import com.fitness.entity.StripePayment
import com.fitness.entity.TransactionType
import com.fitness.repository.CreditPackageRepository
import com.fitness.repository.CreditTransactionRepository
import com.fitness.repository.StripePaymentRepository
import com.fitness.repository.UserRepository
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.stripe.exception.SignatureVerificationException
import com.stripe.model.Event
import com.stripe.model.checkout.Session
import com.stripe.net.Webhook
import com.stripe.param.checkout.SessionCreateParams
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.*

data class CheckoutResult(
    val sessionId: String,
    val checkoutUrl: String
)

@Service
class StripeService(
    private val stripeConfig: StripeConfig,
    private val stripePaymentRepository: StripePaymentRepository,
    private val creditPackageRepository: CreditPackageRepository,
    private val userRepository: UserRepository,
    private val creditTransactionRepository: CreditTransactionRepository
) {
    private val logger = LoggerFactory.getLogger(StripeService::class.java)

    fun isConfigured(): Boolean = stripeConfig.isConfigured()

    /**
     * Create a Stripe Checkout Session for credit purchase.
     */
    fun createCheckoutSession(
        userId: UUID,
        packageId: UUID,
        userEmail: String
    ): CheckoutResult {
        val creditPackage = creditPackageRepository.findById(packageId)
            .orElseThrow { NoSuchElementException("Credit package not found") }

        val totalCredits = creditPackage.credits

        // Create Stripe Checkout Session
        val params = SessionCreateParams.builder()
            .setMode(SessionCreateParams.Mode.PAYMENT)
            .setCustomerEmail(userEmail)
            .setSuccessUrl("${stripeConfig.successUrl}?session_id={CHECKOUT_SESSION_ID}")
            .setCancelUrl(stripeConfig.cancelUrl)
            .addLineItem(
                SessionCreateParams.LineItem.builder()
                    .setQuantity(1L)
                    .setPriceData(
                        SessionCreateParams.LineItem.PriceData.builder()
                            .setCurrency(creditPackage.currency?.lowercase() ?: "czk")
                            .setUnitAmount(creditPackage.priceCzk.toLong() * 100)  // Convert to cents/halere
                            .setProductData(
                                SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                    .setName(creditPackage.nameCs)
                                    .setDescription("$totalCredits kreditů")
                                    .build()
                            )
                            .build()
                    )
                    .build()
            )
            .putMetadata("user_id", userId.toString())
            .putMetadata("package_id", packageId.toString())
            .putMetadata("credits", totalCredits.toString())
            .build()

        val session = Session.create(params)

        // Save payment record
        stripePaymentRepository.save(
            StripePayment(
                userId = userId,
                stripeSessionId = session.id,
                stripePaymentIntentId = session.paymentIntent,
                amount = creditPackage.priceCzk,
                currency = creditPackage.currency ?: "CZK",
                status = "pending",
                creditPackageId = packageId
            )
        )

        logger.info("Stripe checkout session created: sessionId=${session.id}, userId=$userId")

        return CheckoutResult(
            sessionId = session.id,
            checkoutUrl = session.url
        )
    }

    /**
     * Verify webhook signature and parse event.
     */
    fun verifyWebhookSignature(payload: String, signature: String): Event? {
        return try {
            Webhook.constructEvent(payload, signature, stripeConfig.webhookSecret)
        } catch (e: SignatureVerificationException) {
            logger.error("Invalid Stripe webhook signature: ${e.message}")
            null
        }
    }

    /**
     * Handle Stripe webhook event.
     */
    @Transactional
    fun handleWebhookEvent(event: Event): Boolean {
        logger.info("Processing Stripe webhook: type=${event.type}, id=${event.id}")

        return when (event.type) {
            "checkout.session.completed" -> handleCheckoutCompleted(event)
            "checkout.session.expired" -> handleCheckoutExpired(event)
            else -> {
                logger.debug("Unhandled event type: ${event.type}")
                true
            }
        }
    }

    private fun handleCheckoutCompleted(event: Event): Boolean {
        // Use Stripe SDK's proper deserialization instead of regex
        val session = extractSessionFromEvent(event)
        if (session == null) {
            logger.error("Failed to deserialize checkout session from event: eventId=${event.id}")
            return false
        }

        val sessionId = session.id
        if (sessionId.isNullOrBlank()) {
            logger.error("Session ID is missing from deserialized session: eventId=${event.id}")
            return false
        }

        logger.info("Processing checkout session: $sessionId")

        val payment = stripePaymentRepository.findByStripeSessionId(sessionId)
        if (payment == null) {
            logger.error("Payment not found for session: $sessionId")
            return false
        }

        // Idempotency check
        if (payment.status == "completed") {
            logger.debug("Payment already processed: $sessionId")
            return true
        }

        // Get payment intent ID from the webhook session data, or retrieve from Stripe API if missing
        val paymentIntentId = session.paymentIntent ?: try {
            Session.retrieve(sessionId).paymentIntent
        } catch (e: Exception) {
            logger.warn("Could not retrieve payment intent from Stripe: ${e.message}")
            null
        }

        // Update payment status
        val updatedPayment = payment.copy(
            status = "completed",
            stripePaymentIntentId = paymentIntentId,
            updatedAt = Instant.now()
        )
        stripePaymentRepository.save(updatedPayment)

        // Add credits to user
        if (payment.creditPackageId != null && payment.userId != null) {
            val creditPackage = creditPackageRepository.findById(payment.creditPackageId).orElse(null)
            if (creditPackage != null) {
                val totalCredits = creditPackage.credits
                userRepository.updateCredits(payment.userId, totalCredits)

                creditTransactionRepository.save(
                    CreditTransaction(
                        userId = payment.userId,
                        amount = totalCredits,
                        type = TransactionType.PURCHASE.value,
                        stripePaymentId = sessionId,
                        note = "Nákup: ${creditPackage.nameCs}"
                    )
                )

                logger.info("Credits added: userId=${payment.userId}, credits=$totalCredits")
            }
        }

        return true
    }

    private fun handleCheckoutExpired(event: Event): Boolean {
        val session = extractSessionFromEvent(event)
        if (session == null) {
            logger.error("Failed to deserialize checkout session from expired event: eventId=${event.id}")
            return false
        }

        val sessionId = session.id
        if (sessionId.isNullOrBlank()) {
            logger.error("Session ID is missing from deserialized expired session: eventId=${event.id}")
            return false
        }

        val payment = stripePaymentRepository.findByStripeSessionId(sessionId)
            ?: return true  // Already cleaned up or never existed

        val updatedPayment = payment.copy(
            status = "expired",
            updatedAt = Instant.now()
        )
        stripePaymentRepository.save(updatedPayment)

        logger.info("Checkout session expired: $sessionId")
        return true
    }

    /**
     * Extract checkout session from webhook event using proper Stripe SDK deserialization.
     * Falls back to API retrieval with Jackson parsing if SDK deserialization fails.
     */
    private fun extractSessionFromEvent(event: Event): Session? {
        val deserializer = event.dataObjectDeserializer

        // First, try Stripe SDK's built-in deserialization
        if (deserializer.`object`.isPresent) {
            val stripeObject = deserializer.`object`.get()
            if (stripeObject is Session) {
                return stripeObject
            }
            logger.warn("Unexpected object type from deserializer: ${stripeObject.javaClass.simpleName}")
        }

        // Fallback: Parse raw JSON with Jackson if SDK deserialization fails
        logger.debug("SDK deserialization unavailable, parsing raw JSON for event: ${event.id}")
        return try {
            val objectMapper = ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            objectMapper.readValue(deserializer.rawJson, Session::class.java)
        } catch (e: Exception) {
            logger.error("Failed to parse session from raw JSON: ${e.message}", e)
            null
        }
    }

    fun getPaymentBySessionId(sessionId: String): StripePayment? {
        return stripePaymentRepository.findByStripeSessionId(sessionId)
    }
}
