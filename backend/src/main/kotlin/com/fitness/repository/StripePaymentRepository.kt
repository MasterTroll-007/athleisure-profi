package com.fitness.repository

import com.fitness.entity.StripePayment
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface StripePaymentRepository : JpaRepository<StripePayment, UUID> {
    fun findByStripeSessionId(stripeSessionId: String): StripePayment?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM StripePayment p WHERE p.stripeSessionId = :stripeSessionId")
    fun findByStripeSessionIdForUpdate(stripeSessionId: String): StripePayment?

    fun findByStripePaymentIntentId(stripePaymentIntentId: String): StripePayment?
    fun findByUserId(userId: UUID): List<StripePayment>
    fun findAllByOrderByCreatedAtDesc(): List<StripePayment>

    @Query("""
        SELECT p FROM StripePayment p
        WHERE p.userId IN (SELECT u.id FROM User u WHERE u.trainerId = :trainerId)
           OR p.creditPackageId IN (SELECT c.id FROM CreditPackage c WHERE c.trainerId = :trainerId)
        ORDER BY p.createdAt DESC
    """)
    fun findByTrainerIdOrderByCreatedAtDesc(trainerId: UUID): List<StripePayment>
}
