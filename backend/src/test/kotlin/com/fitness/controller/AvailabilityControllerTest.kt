package com.fitness.controller

import com.fitness.dto.CreateAvailabilityBlockRequest
import com.fitness.entity.AvailabilityBlock
import com.fitness.entity.User
import com.fitness.repository.AvailabilityBlockRepository
import com.fitness.repository.UserRepository
import com.fitness.security.UserPrincipal
import com.fitness.service.AvailabilityBlockValidationService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import java.time.LocalTime
import java.util.Optional
import java.util.UUID

class AvailabilityControllerTest {
    private val blockRepository = mockk<AvailabilityBlockRepository>(relaxed = true)
    private val userRepository = mockk<UserRepository>()
    private val validationService = mockk<AvailabilityBlockValidationService>()
    private val controller = AvailabilityController(blockRepository, userRepository, validationService)

    @Test
    fun `active blocks return empty response without authenticated user`() {
        val response = controller.getActiveBlocks(null)

        assertThat(response.body).isEmpty()
    }

    @Test
    fun `active blocks use own admin id for admins and trainer id for clients`() {
        val adminId = UUID.randomUUID()
        val clientId = UUID.randomUUID()
        every { userRepository.findById(adminId) } returns Optional.of(user(adminId, role = "admin"))
        every { userRepository.findById(clientId) } returns Optional.of(user(clientId, trainerId = adminId))
        every { blockRepository.findByIsActiveTrueAndAdminId(adminId) } returns listOf(
            block(id = UUID.randomUUID(), adminId = adminId, daysOfWeek = "1,3")
        )

        val adminBlocks = controller.getActiveBlocks(principal(adminId, role = "admin")).body!!
        val clientBlocks = controller.getActiveBlocks(principal(clientId)).body!!

        assertThat(adminBlocks).hasSize(1)
        assertThat(clientBlocks).hasSize(1)
        assertThat(clientBlocks.single().daysOfWeek).containsExactly(1, 3)
    }

    @Test
    fun `creating non blocked overlapping block returns conflict payload`() {
        val adminId = UUID.randomUUID()
        val existing = block(id = UUID.randomUUID(), adminId = adminId)
        every {
            validationService.checkForOverlappingBlocks(
                daysOfWeek = listOf(1),
                startTime = LocalTime.of(10, 0),
                endTime = LocalTime.of(11, 0),
                adminId = adminId
            )
        } returns existing
        every { validationService.formatOverlapError(existing) } returns mapOf("error" to "OVERLAPPING_BLOCK")

        val response = controller.createBlock(
            principal(adminId, role = "admin"),
            CreateAvailabilityBlockRequest(
                name = "Morning",
                daysOfWeek = listOf(1),
                startTime = "10:00",
                endTime = "11:00",
                isBlocked = false
            )
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.CONFLICT)
        assertThat(response.body).isEqualTo(mapOf("error" to "OVERLAPPING_BLOCK"))
    }

    @Test
    fun `creating blocked block skips overlap check and stores admin id`() {
        val adminId = UUID.randomUUID()
        val savedId = UUID.randomUUID()
        every { blockRepository.save(any()) } answers {
            firstArg<AvailabilityBlock>().copy(id = savedId)
        }

        val response = controller.createBlock(
            principal(adminId, role = "admin"),
            CreateAvailabilityBlockRequest(
                name = "Blocked",
                daysOfWeek = listOf(2),
                startTime = "12:00",
                endTime = "13:00",
                isBlocked = true
            )
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
        val dto = response.body as com.fitness.dto.AvailabilityBlockDTO
        assertThat(dto.id).isEqualTo(savedId.toString())
        assertThat(dto.isBlocked).isTrue()
        verify(exactly = 0) {
            validationService.checkForOverlappingBlocks(any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `calendar settings return trainer settings for clients and own settings for admins`() {
        val adminId = UUID.randomUUID()
        val clientId = UUID.randomUUID()
        every { userRepository.findById(adminId) } returns Optional.of(
            user(adminId, role = "admin", startHour = 8, endHour = 19, adjacentBookingRequired = false)
        )
        every { userRepository.findById(clientId) } returns Optional.of(user(clientId, trainerId = adminId))

        val adminSettings = controller.getCalendarSettings(principal(adminId, role = "admin")).body!!
        val clientSettings = controller.getCalendarSettings(principal(clientId)).body!!

        assertThat(adminSettings.calendarStartHour).isEqualTo(8)
        assertThat(adminSettings.calendarEndHour).isEqualTo(19)
        assertThat(adminSettings.adjacentBookingRequired).isFalse()
        assertThat(clientSettings.calendarStartHour).isEqualTo(8)
        assertThat(clientSettings.calendarEndHour).isEqualTo(19)
        assertThat(clientSettings.adjacentBookingRequired).isFalse()
    }

    private fun principal(id: UUID, role: String = "client") = UserPrincipal(
        userId = id.toString(),
        email = "$id@test.com",
        role = role
    )

    private fun user(
        id: UUID,
        role: String = "client",
        trainerId: UUID? = null,
        startHour: Int = 6,
        endHour: Int = 22,
        adjacentBookingRequired: Boolean = true
    ) = User(
        id = id,
        email = "$id@test.com",
        passwordHash = "hash",
        role = role,
        trainerId = trainerId,
        calendarStartHour = startHour,
        calendarEndHour = endHour,
        adjacentBookingRequired = adjacentBookingRequired
    )

    private fun block(
        id: UUID,
        adminId: UUID,
        daysOfWeek: String = "1",
        start: LocalTime = LocalTime.of(10, 0),
        end: LocalTime = LocalTime.of(11, 0)
    ) = AvailabilityBlock(
        id = id,
        name = "Block",
        daysOfWeek = daysOfWeek,
        startTime = start,
        endTime = end,
        adminId = adminId,
        isActive = true
    )
}
