package com.fitness.mapper

import com.fitness.entity.CreditPackage
import com.fitness.entity.StripePayment
import com.fitness.entity.User
import com.fitness.repository.CreditPackageRepository
import com.fitness.repository.UserRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant
import java.util.Optional
import java.util.UUID

class PaymentMapperTest {

    private val userRepository = mockk<UserRepository>()
    private val creditPackageRepository = mockk<CreditPackageRepository>()
    private val mapper = PaymentMapper(userRepository, creditPackageRepository)

    @Test
    fun `mapStripeStatusToLegacy keeps frontend-compatible states`() {
        assertEquals("PAID", mapper.mapStripeStatusToLegacy("completed"))
        assertEquals("CREATED", mapper.mapStripeStatusToLegacy("pending"))
        assertEquals("TIMEOUTED", mapper.mapStripeStatusToLegacy("expired"))
        assertEquals("REFUNDED", mapper.mapStripeStatusToLegacy("refunded"))
        assertEquals("REQUIRES_PAYMENT_METHOD", mapper.mapStripeStatusToLegacy("requires_payment_method"))
    }

    @Test
    fun `toAdminDTO maps payment with prefetched user and package`() {
        val now = Instant.parse("2026-04-18T09:00:00Z")
        val userId = UUID.randomUUID()
        val packageId = UUID.randomUUID()
        val payment = payment(
            id = UUID.randomUUID(),
            userId = userId,
            creditPackageId = packageId,
            status = "completed",
            createdAt = now,
            updatedAt = now.plusSeconds(60),
        )
        val user = user(userId, firstName = "Eva", lastName = "Novak")
        val creditPackage = creditPackage(packageId, nameCs = "10 kreditu")

        val dto = mapper.toAdminDTO(payment, user, creditPackage)

        assertEquals(payment.id.toString(), dto.id)
        assertEquals(userId.toString(), dto.userId)
        assertEquals("Eva Novak", dto.userName)
        assertNull(dto.gopayId)
        assertEquals("cs_test", dto.stripeSessionId)
        assertEquals(BigDecimal("1500.00"), dto.amount)
        assertEquals("CZK", dto.currency)
        assertEquals("PAID", dto.state)
        assertEquals(packageId.toString(), dto.creditPackageId)
        assertEquals("10 kreditu", dto.creditPackageName)
        assertEquals(now.toString(), dto.createdAt)
        assertEquals(now.plusSeconds(60).toString(), dto.updatedAt)
    }

    @Test
    fun `toAdminDTO fetches user and package when only ids are present`() {
        val userId = UUID.randomUUID()
        val packageId = UUID.randomUUID()
        val payment = payment(
            id = UUID.randomUUID(),
            userId = userId,
            creditPackageId = packageId,
            status = "pending",
        )
        every { userRepository.findById(userId) } returns Optional.of(user(userId, firstName = "Eva", lastName = null))
        every { creditPackageRepository.findById(packageId) } returns Optional.of(creditPackage(packageId, nameCs = "Start"))

        val dto = mapper.toAdminDTO(payment)

        assertEquals("Eva", dto.userName)
        assertEquals("Start", dto.creditPackageName)
        assertEquals("CREATED", dto.state)
    }

    @Test
    fun `toAdminDTOBatch fetches related users and packages once`() {
        val userId = UUID.randomUUID()
        val missingUserId = UUID.randomUUID()
        val packageId = UUID.randomUUID()
        val missingPackageId = UUID.randomUUID()
        val payments = listOf(
            payment(id = UUID.randomUUID(), userId = userId, creditPackageId = packageId, status = "completed"),
            payment(id = UUID.randomUUID(), userId = missingUserId, creditPackageId = missingPackageId, status = "expired"),
            payment(id = UUID.randomUUID(), userId = null, creditPackageId = null, status = "refunded"),
        )
        every { userRepository.findAllById(any<Iterable<UUID>>()) } returns listOf(user(userId, firstName = "Eva", lastName = "Novak"))
        every { creditPackageRepository.findAllById(any<Iterable<UUID>>()) } returns listOf(creditPackage(packageId, nameCs = "Start"))

        val dtos = mapper.toAdminDTOBatch(payments)

        assertEquals(listOf("Eva Novak", null, null), dtos.map { it.userName })
        assertEquals(listOf("Start", null, null), dtos.map { it.creditPackageName })
        assertEquals(listOf("PAID", "TIMEOUTED", "REFUNDED"), dtos.map { it.state })
        verify(exactly = 1) { userRepository.findAllById(any<Iterable<UUID>>()) }
        verify(exactly = 1) { creditPackageRepository.findAllById(any<Iterable<UUID>>()) }
    }

    private fun payment(
        id: UUID,
        userId: UUID?,
        creditPackageId: UUID?,
        status: String,
        createdAt: Instant = Instant.parse("2026-04-18T09:00:00Z"),
        updatedAt: Instant = Instant.parse("2026-04-18T09:00:00Z"),
    ) = StripePayment(
        id = id,
        userId = userId,
        stripeSessionId = "cs_test",
        stripePaymentIntentId = "pi_test",
        amount = BigDecimal("1500.00"),
        currency = "CZK",
        status = status,
        creditPackageId = creditPackageId,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    private fun user(
        id: UUID,
        firstName: String?,
        lastName: String?,
    ) = User(
        id = id,
        email = "client@example.com",
        passwordHash = "hash",
        firstName = firstName,
        lastName = lastName,
    )

    private fun creditPackage(
        id: UUID,
        nameCs: String,
    ) = CreditPackage(
        id = id,
        nameCs = nameCs,
        credits = 10,
        priceCzk = BigDecimal("1500.00"),
    )
}
