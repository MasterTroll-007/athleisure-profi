package com.fitness

import at.favre.lib.crypto.bcrypt.BCrypt
import com.fitness.entity.Slot
import com.fitness.entity.SlotStatus
import com.fitness.entity.TrainingLocation
import com.fitness.entity.User
import java.time.LocalDate
import java.time.LocalTime

/**
 * Reusable builders for integration-test entities. Keeps test arrange
 * blocks terse and avoids drift across files when the entity gains fields.
 */
object TestFixtures {

    private val DEFAULT_BCRYPT = BCrypt.withDefaults().hashToString(4, "Password1!".toCharArray())

    fun user(
        email: String = "user-${System.nanoTime()}@test.com",
        role: String = "client",
        credits: Int = 10,
        emailVerified: Boolean = true,
        isBlocked: Boolean = false,
        trainerId: java.util.UUID? = null,
        passwordHash: String = DEFAULT_BCRYPT,
    ) = User(
        email = email,
        passwordHash = passwordHash,
        firstName = "Test",
        lastName = "User",
        role = role,
        credits = credits,
        emailVerified = emailVerified,
        isBlocked = isBlocked,
        trainerId = trainerId,
    )

    fun adminUser(email: String = "admin-${System.nanoTime()}@test.com") =
        user(email = email, role = "admin")

    fun slot(
        date: LocalDate = LocalDate.now().plusDays(7),
        start: LocalTime = LocalTime.of(10, 0),
        durationMinutes: Int = 60,
        status: SlotStatus = SlotStatus.UNLOCKED,
        adminId: java.util.UUID? = null,
        capacity: Int = 1,
        locationId: java.util.UUID? = null,
    ) = Slot(
        date = date,
        startTime = start,
        endTime = start.plusMinutes(durationMinutes.toLong()),
        durationMinutes = durationMinutes,
        status = status,
        adminId = adminId,
        capacity = capacity,
        locationId = locationId,
    )

    fun location(
        nameCs: String = "Test Gym",
        color: String = "#3B82F6",
        adminId: java.util.UUID? = null,
    ) = TrainingLocation(
        nameCs = nameCs,
        color = color,
        adminId = adminId,
    )
}
