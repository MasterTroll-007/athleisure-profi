package com.fitness.service

import com.fitness.entity.PricingItem
import com.fitness.entity.TrainingLocation
import com.fitness.entity.User
import com.fitness.repository.PricingItemRepository
import com.fitness.repository.TrainingLocationRepository
import com.fitness.repository.UserRepository
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.security.access.AccessDeniedException
import java.time.LocalTime
import java.util.Optional
import java.util.UUID

class SlotPolicyServiceTest {
    private val userRepository = mockk<UserRepository>()
    private val locationRepository = mockk<TrainingLocationRepository>()
    private val pricingItemRepository = mockk<PricingItemRepository>()
    private val service = SlotPolicyService(userRepository, locationRepository, pricingItemRepository)

    @Test
    fun `client ownership allows null and own client but rejects foreign client`() {
        val adminId = UUID.randomUUID()
        val clientId = UUID.randomUUID()

        service.requireClientOwnedByAdmin(null, adminId)

        every { userRepository.findById(clientId) } returns Optional.of(user(clientId, trainerId = adminId))
        service.requireClientOwnedByAdmin(clientId, adminId)

        every { userRepository.findById(clientId) } returns Optional.of(user(clientId, trainerId = UUID.randomUUID()))
        assertThatThrownBy { service.requireClientOwnedByAdmin(clientId, adminId) }
            .isInstanceOf(AccessDeniedException::class.java)
    }

    @Test
    fun `location ownership allows null and own location but rejects foreign location`() {
        val adminId = UUID.randomUUID()
        val locationId = UUID.randomUUID()

        service.requireLocationOwnedByAdmin(null, adminId)

        every { locationRepository.findById(locationId) } returns Optional.of(
            TrainingLocation(id = locationId, nameCs = "Gym", color = "#336699", adminId = adminId)
        )
        service.requireLocationOwnedByAdmin(locationId, adminId)

        every { locationRepository.findById(locationId) } returns Optional.of(
            TrainingLocation(id = locationId, nameCs = "Other Gym", color = "#663399", adminId = UUID.randomUUID())
        )
        assertThatThrownBy { service.requireLocationOwnedByAdmin(locationId, adminId) }
            .isInstanceOf(AccessDeniedException::class.java)
    }

    @Test
    fun `pricing item ownership validates distinct ids and rejects missing or foreign items`() {
        val adminId = UUID.randomUUID()
        val ownId = UUID.randomUUID()
        val foreignId = UUID.randomUUID()

        service.requirePricingItemsOwnedByAdmin(emptyList(), adminId)

        every { pricingItemRepository.findAllById(listOf(ownId)) } returns listOf(
            PricingItem(id = ownId, nameCs = "Training", credits = 1, adminId = adminId)
        )
        service.requirePricingItemsOwnedByAdmin(listOf(ownId.toString(), ownId.toString()), adminId)

        every { pricingItemRepository.findAllById(listOf(ownId, foreignId)) } returns listOf(
            PricingItem(id = ownId, nameCs = "Training", credits = 1, adminId = adminId)
        )
        assertThatThrownBy {
            service.requirePricingItemsOwnedByAdmin(listOf(ownId.toString(), foreignId.toString()), adminId)
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("not found")

        every { pricingItemRepository.findAllById(listOf(foreignId)) } returns listOf(
            PricingItem(id = foreignId, nameCs = "Other", credits = 1, adminId = UUID.randomUUID())
        )
        assertThatThrownBy {
            service.requirePricingItemsOwnedByAdmin(listOf(foreignId.toString()), adminId)
        }.isInstanceOf(AccessDeniedException::class.java)
    }

    @Test
    fun `calendar hour check supports normal and midnight end boundaries`() {
        assertThat(
            service.isWithinCalendarHours(LocalTime.of(8, 0), LocalTime.of(9, 0), 8, 18)
        ).isTrue()
        assertThat(
            service.isWithinCalendarHours(LocalTime.of(7, 59), LocalTime.of(9, 0), 8, 18)
        ).isFalse()
        assertThat(
            service.isWithinCalendarHours(LocalTime.of(23, 0), LocalTime.of(23, 59), 6, 24)
        ).isTrue()
    }

    @Test
    fun `admin calendar hour validation uses saved admin settings`() {
        val adminId = UUID.randomUUID()
        every { userRepository.findById(adminId) } returns Optional.of(
            user(adminId, role = "admin", calendarStartHour = 8, calendarEndHour = 18)
        )

        service.requireWithinAdminCalendarHours(adminId, LocalTime.of(8, 0), LocalTime.of(9, 0))

        assertThatThrownBy {
            service.requireWithinAdminCalendarHours(adminId, LocalTime.of(7, 45), LocalTime.of(9, 0))
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("08:00-18:00")
    }

    private fun user(
        id: UUID,
        trainerId: UUID? = null,
        role: String = "client",
        calendarStartHour: Int = 6,
        calendarEndHour: Int = 22
    ) = User(
        id = id,
        email = "$id@test.com",
        passwordHash = "hash",
        role = role,
        trainerId = trainerId,
        calendarStartHour = calendarStartHour,
        calendarEndHour = calendarEndHour
    )
}
