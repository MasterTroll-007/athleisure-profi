package com.fitness.config

import com.fitness.entity.*
import com.fitness.repository.*
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters

@Component
class DataInitializer(
    private val slotRepository: SlotRepository,
    private val reservationRepository: ReservationRepository,
    private val userRepository: UserRepository,
    private val creditTransactionRepository: CreditTransactionRepository
) : CommandLineRunner {

    @Transactional
    override fun run(vararg args: String?) {
        // Only initialize if slots table is empty
        if (slotRepository.count() > 0) {
            println("Slots already exist, skipping data initialization")
            return
        }

        println("Initializing test data: generating slots and reservations...")

        // Get test users
        val testUsers = userRepository.findAll()
            .filter { it.email.startsWith("test") && it.email.endsWith("@test.com") }
            .take(5)

        if (testUsers.isEmpty()) {
            println("No test users found, skipping reservation generation")
            return
        }

        // Generate slots for current week + next week (Monday-Friday, 14:00-18:00)
        val monday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val createdSlots = mutableListOf<Slot>()

        for (weekOffset in 0..1) {
            for (dayOffset in 0..4) { // Monday to Friday
                val slotDate = monday.plusWeeks(weekOffset.toLong()).plusDays(dayOffset.toLong())

                // Only create slots for today or future
                if (!slotDate.isBefore(LocalDate.now())) {
                    // Create 4 hourly slots: 14:00, 15:00, 16:00, 17:00
                    for (hour in 14..17) {
                        val startTime = LocalTime.of(hour, 0)
                        val endTime = LocalTime.of(hour + 1, 0)

                        val slot = slotRepository.save(
                            Slot(
                                date = slotDate,
                                startTime = startTime,
                                endTime = endTime,
                                durationMinutes = 60,
                                status = SlotStatus.UNLOCKED,
                                createdAt = Instant.now()
                            )
                        )
                        createdSlots.add(slot)
                    }
                }
            }
        }

        println("Created ${createdSlots.size} slots")

        // Create reservations for ~50% of slots (every other slot)
        var userIndex = 0
        createdSlots.forEachIndexed { index, slot ->
            if (index % 2 == 0) { // Reserve every other slot
                val user = testUsers[userIndex % testUsers.size]

                // Create reservation
                val reservation = reservationRepository.save(
                    Reservation(
                        userId = user.id,
                        slotId = slot.id,
                        date = slot.date,
                        startTime = slot.startTime,
                        endTime = slot.endTime,
                        status = "confirmed",
                        creditsUsed = 1,
                        createdAt = Instant.now()
                    )
                )

                // Update slot status to RESERVED
                slotRepository.save(slot.copy(status = SlotStatus.RESERVED))

                // Deduct credit from user
                userRepository.updateCredits(user.id, -1)

                // Create credit transaction
                creditTransactionRepository.save(
                    CreditTransaction(
                        userId = user.id,
                        amount = -1,
                        type = TransactionType.RESERVATION.value,
                        referenceId = reservation.id,
                        note = "Rezervace na ${slot.date}",
                        createdAt = Instant.now()
                    )
                )

                userIndex++
            }
        }

        println("Created ${createdSlots.size / 2} reservations (~50% occupancy)")
        println("Data initialization completed successfully")
    }
}
