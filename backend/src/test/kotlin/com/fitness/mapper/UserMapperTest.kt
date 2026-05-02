package com.fitness.mapper

import com.fitness.entity.User
import com.fitness.repository.UserRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.Optional
import java.util.UUID

class UserMapperTest {

    private val userRepository = mockk<UserRepository>()
    private val mapper = UserMapper(userRepository)

    @Test
    fun `toDTO copies profile fields and resolves trainer name`() {
        val userId = UUID.randomUUID()
        val trainerId = UUID.randomUUID()
        val createdAt = Instant.parse("2026-04-18T08:15:30Z")
        val trainer = user(
            id = trainerId,
            email = "trainer@example.com",
            firstName = "Jana",
            lastName = "Pankova",
        )
        val client = user(
            id = userId,
            email = "client@example.com",
            firstName = "Eva",
            lastName = "Novak",
            phone = "+420 123 456 789",
            credits = 12,
            locale = "en",
            theme = "dark",
            trainerId = trainerId,
            calendarStartHour = 7,
            calendarEndHour = 21,
            isBlocked = true,
            emailRemindersEnabled = false,
            reminderHoursBefore = 6,
            createdAt = createdAt,
        )
        every { userRepository.findById(trainerId) } returns Optional.of(trainer)

        val dto = mapper.toDTO(client)

        assertEquals(userId.toString(), dto.id)
        assertEquals("client@example.com", dto.email)
        assertEquals("Eva", dto.firstName)
        assertEquals("Novak", dto.lastName)
        assertEquals("+420 123 456 789", dto.phone)
        assertEquals("client", dto.role)
        assertEquals(12, dto.credits)
        assertEquals("en", dto.locale)
        assertEquals("dark", dto.theme)
        assertEquals(trainerId.toString(), dto.trainerId)
        assertEquals("Jana Pankova", dto.trainerName)
        assertEquals(7, dto.calendarStartHour)
        assertEquals(21, dto.calendarEndHour)
        assertEquals(true, dto.isBlocked)
        assertEquals(false, dto.emailRemindersEnabled)
        assertEquals(6, dto.reminderHoursBefore)
        assertEquals(createdAt.toString(), dto.createdAt)
    }

    @Test
    fun `toDTOBatch fetches distinct trainers once and preserves user order`() {
        val trainerOneId = UUID.randomUUID()
        val trainerTwoId = UUID.randomUUID()
        val trainerOne = user(
            id = trainerOneId,
            email = "trainer.one@example.com",
            firstName = "Jana",
            lastName = "Pankova",
        )
        val trainerTwo = user(id = trainerTwoId, email = "fallback@example.com")
        val clients = listOf(
            user(id = UUID.randomUUID(), email = "a@example.com", trainerId = trainerOneId),
            user(id = UUID.randomUUID(), email = "b@example.com", trainerId = trainerOneId),
            user(id = UUID.randomUUID(), email = "c@example.com", trainerId = trainerTwoId),
            user(id = UUID.randomUUID(), email = "d@example.com"),
        )
        every { userRepository.findAllById(any<Iterable<UUID>>()) } returns listOf(trainerOne, trainerTwo)

        val dtos = mapper.toDTOBatch(clients)

        assertEquals(listOf("a@example.com", "b@example.com", "c@example.com", "d@example.com"), dtos.map { it.email })
        assertEquals(listOf("Jana Pankova", "Jana Pankova", "fallback@example.com", null), dtos.map { it.trainerName })
        verify(exactly = 1) { userRepository.findAllById(any<Iterable<UUID>>()) }
    }

    @Test
    fun `trainer DTO helpers keep only public trainer data`() {
        val trainer = user(
            id = UUID.randomUUID(),
            email = "trainer@example.com",
            firstName = "Jana",
            lastName = "Pankova",
            role = "admin",
            calendarStartHour = 6,
            calendarEndHour = 20,
        )

        val trainerDto = mapper.toTrainerDTO(trainer)
        val infoDto = mapper.toTrainerInfoDTO(trainer)

        assertEquals(trainer.id.toString(), trainerDto.id)
        assertEquals("trainer@example.com", trainerDto.email)
        assertEquals("Jana", trainerDto.firstName)
        assertEquals("Pankova", trainerDto.lastName)
        assertEquals(6, trainerDto.calendarStartHour)
        assertEquals(20, trainerDto.calendarEndHour)
        assertEquals("Jana", infoDto.firstName)
        assertEquals("Pankova", infoDto.lastName)
    }

    @Test
    fun `resolveTrainerNames maps ids to formatted names and skips empty input`() {
        val namedTrainerId = UUID.randomUUID()
        val emailOnlyTrainerId = UUID.randomUUID()
        every { userRepository.findAllById(any<Iterable<UUID>>()) } returns listOf(
            user(id = namedTrainerId, email = "named@example.com", firstName = "Jana", lastName = "Pankova"),
            user(id = emailOnlyTrainerId, email = "email.only@example.com"),
        )

        val resolved = mapper.resolveTrainerNames(setOf(namedTrainerId, emailOnlyTrainerId))

        assertEquals("Jana Pankova", resolved[namedTrainerId])
        assertEquals("email.only@example.com", resolved[emailOnlyTrainerId])
        assertEquals(emptyMap<UUID, String>(), mapper.resolveTrainerNames(emptySet()))
    }

    @Test
    fun `formatTrainerName handles null and missing name`() {
        assertNull(mapper.formatTrainerName(null))
        assertEquals("trainer@example.com", mapper.formatTrainerName(user(id = UUID.randomUUID(), email = "trainer@example.com")))
    }

    private fun user(
        id: UUID,
        email: String,
        firstName: String? = null,
        lastName: String? = null,
        phone: String? = null,
        role: String = "client",
        credits: Int = 0,
        locale: String = "cs",
        theme: String = "system",
        trainerId: UUID? = null,
        calendarStartHour: Int = 6,
        calendarEndHour: Int = 22,
        isBlocked: Boolean = false,
        emailRemindersEnabled: Boolean = true,
        reminderHoursBefore: Int = 24,
        createdAt: Instant = Instant.parse("2026-04-18T08:15:30Z"),
    ) = User(
        id = id,
        email = email,
        passwordHash = "hash",
        firstName = firstName,
        lastName = lastName,
        phone = phone,
        role = role,
        credits = credits,
        locale = locale,
        theme = theme,
        trainerId = trainerId,
        calendarStartHour = calendarStartHour,
        calendarEndHour = calendarEndHour,
        isBlocked = isBlocked,
        emailRemindersEnabled = emailRemindersEnabled,
        reminderHoursBefore = reminderHoursBefore,
        createdAt = createdAt,
    )
}
