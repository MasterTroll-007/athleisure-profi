package com.fitness.service

import com.fitness.entity.AvailabilityBlock
import com.fitness.repository.AvailabilityBlockRepository
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.DayOfWeek
import java.time.LocalTime
import java.util.UUID

class AvailabilityBlockValidationServiceTest {
    private val repository = mockk<AvailabilityBlockRepository>()
    private val service = AvailabilityBlockValidationService(repository)

    @Test
    fun `overlap detection uses admin scoped active unblocked blocks`() {
        val adminId = UUID.randomUUID()
        val block = block(daysOfWeek = "1,3", start = LocalTime.of(10, 0), end = LocalTime.of(11, 0))
        every { repository.findByIsActiveTrueAndAdminId(adminId) } returns listOf(block)

        val result = service.checkForOverlappingBlocks(
            daysOfWeek = listOf(3),
            startTime = LocalTime.of(10, 30),
            endTime = LocalTime.of(11, 30),
            adminId = adminId
        )

        assertThat(result).isEqualTo(block)
    }

    @Test
    fun `overlap detection supports single day enum fallback`() {
        val block = block(daysOfWeek = "", dayOfWeek = DayOfWeek.FRIDAY)
        every { repository.findByIsActiveTrue() } returns listOf(block)

        val result = service.checkForOverlappingBlocks(
            daysOfWeek = listOf(5),
            startTime = LocalTime.of(10, 30),
            endTime = LocalTime.of(11, 30)
        )

        assertThat(result).isEqualTo(block)
    }

    @Test
    fun `overlap detection ignores excluded blocked non-overlapping and different-day blocks`() {
        val excludedId = UUID.randomUUID()
        val ignored = listOf(
            block(id = excludedId, daysOfWeek = "1"),
            block(daysOfWeek = "1", isBlocked = true),
            block(daysOfWeek = "1", start = LocalTime.of(12, 0), end = LocalTime.of(13, 0)),
            block(daysOfWeek = "2", start = LocalTime.of(10, 0), end = LocalTime.of(11, 0))
        )
        every { repository.findByIsActiveTrue() } returns ignored

        val result = service.checkForOverlappingBlocks(
            daysOfWeek = listOf(1),
            startTime = LocalTime.of(10, 30),
            endTime = LocalTime.of(11, 30),
            excludeBlockId = excludedId
        )

        assertThat(result).isNull()
    }

    @Test
    fun `overlap error exposes stable code and block time`() {
        val result = service.formatOverlapError(
            block(name = "Morning", start = LocalTime.of(8, 0), end = LocalTime.of(9, 0))
        )

        assertThat(result["error"]).isEqualTo("OVERLAPPING_BLOCK")
        assertThat(result["message"]).contains("Morning", "08:00", "09:00")
    }

    private fun block(
        id: UUID = UUID.randomUUID(),
        name: String? = "Existing",
        daysOfWeek: String = "1",
        dayOfWeek: DayOfWeek? = null,
        start: LocalTime = LocalTime.of(10, 0),
        end: LocalTime = LocalTime.of(11, 0),
        isBlocked: Boolean? = false
    ) = AvailabilityBlock(
        id = id,
        name = name,
        daysOfWeek = daysOfWeek,
        dayOfWeek = dayOfWeek,
        startTime = start,
        endTime = end,
        isBlocked = isBlocked
    )
}
