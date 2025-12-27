package com.fitness.repository

import com.fitness.entity.StripePayment
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface StripePaymentRepository : JpaRepository<StripePayment, UUID> {
    fun findByStripeSessionId(stripeSessionId: String): StripePayment?
    fun findByStripePaymentIntentId(stripePaymentIntentId: String): StripePayment?
    fun findByUserId(userId: UUID): List<StripePayment>
    fun findAllByOrderByCreatedAtDesc(): List<StripePayment>
}
