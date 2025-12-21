package com.fitness.repositories

import com.fitness.models.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.lessEq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.util.*

object ReservationRepository {

    fun findById(id: UUID): ReservationDTO? = transaction {
        (Reservations innerJoin Users)
            .leftJoin(PricingItems)
            .select(Reservations.id eq id)
            .map { it.toReservationDTO() }
            .singleOrNull()
    }

    fun findByUser(userId: UUID): List<ReservationDTO> = transaction {
        (Reservations innerJoin Users)
            .leftJoin(PricingItems)
            .select(Reservations.userId eq userId)
            .orderBy(Reservations.date, SortOrder.DESC)
            .orderBy(Reservations.startTime, SortOrder.DESC)
            .map { it.toReservationDTO() }
    }

    fun findUpcomingByUser(userId: UUID): List<ReservationDTO> = transaction {
        val today = LocalDate.now()
        (Reservations innerJoin Users)
            .leftJoin(PricingItems)
            .select {
                (Reservations.userId eq userId) and
                (Reservations.date greaterEq today) and
                (Reservations.status eq "confirmed")
            }
            .orderBy(Reservations.date, SortOrder.ASC)
            .orderBy(Reservations.startTime, SortOrder.ASC)
            .map { it.toReservationDTO() }
    }

    fun findByDate(date: LocalDate): List<ReservationDTO> = transaction {
        (Reservations innerJoin Users)
            .leftJoin(PricingItems)
            .select {
                (Reservations.date eq date) and
                (Reservations.status eq "confirmed")
            }
            .orderBy(Reservations.startTime, SortOrder.ASC)
            .map { it.toReservationDTO() }
    }

    fun findByDateRange(startDate: LocalDate, endDate: LocalDate): List<ReservationDTO> = transaction {
        (Reservations innerJoin Users)
            .leftJoin(PricingItems)
            .select {
                (Reservations.date greaterEq startDate) and
                (Reservations.date lessEq endDate) and
                (Reservations.status eq "confirmed")
            }
            .orderBy(Reservations.date, SortOrder.ASC)
            .orderBy(Reservations.startTime, SortOrder.ASC)
            .map { it.toReservationDTO() }
    }

    fun findConfirmedByDateAndBlock(date: LocalDate, blockId: UUID): List<ReservationTimeSlot> = transaction {
        Reservations.select {
            (Reservations.date eq date) and
            (Reservations.blockId eq blockId) and
            (Reservations.status eq "confirmed")
        }
            .map {
                ReservationTimeSlot(
                    startTime = it[Reservations.startTime],
                    endTime = it[Reservations.endTime]
                )
            }
    }

    fun create(
        userId: UUID,
        blockId: UUID,
        date: LocalDate,
        startTime: LocalTime,
        endTime: LocalTime,
        creditsUsed: Int,
        pricingItemId: UUID?
    ): ReservationDTO = transaction {
        val id = Reservations.insertAndGetId {
            it[Reservations.userId] = userId
            it[Reservations.blockId] = blockId
            it[Reservations.date] = date
            it[Reservations.startTime] = startTime
            it[Reservations.endTime] = endTime
            it[Reservations.creditsUsed] = creditsUsed
            it[Reservations.pricingItemId] = pricingItemId
            it[status] = "confirmed"
            it[createdAt] = Instant.now()
        }

        findById(id.value)!!
    }

    fun cancel(id: UUID): Boolean = transaction {
        Reservations.update({ Reservations.id eq id }) {
            it[status] = "cancelled"
            it[cancelledAt] = Instant.now()
        } > 0
    }

    fun complete(id: UUID): Boolean = transaction {
        Reservations.update({ Reservations.id eq id }) {
            it[status] = "completed"
        } > 0
    }

    fun getAll(): List<ReservationDTO> = transaction {
        (Reservations innerJoin Users)
            .leftJoin(PricingItems)
            .selectAll()
            .orderBy(Reservations.date, SortOrder.DESC)
            .orderBy(Reservations.startTime, SortOrder.DESC)
            .map { it.toReservationDTO() }
    }

    fun getCalendarEvents(startDate: LocalDate, endDate: LocalDate): List<ReservationCalendarEvent> = transaction {
        (Reservations innerJoin Users)
            .select {
                (Reservations.date greaterEq startDate) and
                (Reservations.date lessEq endDate)
            }
            .map {
                val date = it[Reservations.date]
                val startTime = it[Reservations.startTime]
                val endTime = it[Reservations.endTime]
                val firstName = it[Users.firstName] ?: ""
                val lastName = it[Users.lastName] ?: ""

                ReservationCalendarEvent(
                    id = it[Reservations.id].value.toString(),
                    title = "$firstName $lastName".trim().ifEmpty { it[Users.email] },
                    start = "${date}T${startTime}",
                    end = "${date}T${endTime}",
                    status = it[Reservations.status],
                    clientName = "$firstName $lastName".trim().ifEmpty { null },
                    clientEmail = it[Users.email]
                )
            }
    }

    fun countByUserInDateRange(userId: UUID, startDate: LocalDate, endDate: LocalDate): Int = transaction {
        Reservations.select {
            (Reservations.userId eq userId) and
            (Reservations.date greaterEq startDate) and
            (Reservations.date lessEq endDate) and
            (Reservations.status eq "confirmed")
        }.count().toInt()
    }

    private fun ResultRow.toReservationDTO(): ReservationDTO {
        val firstName = this[Users.firstName] ?: ""
        val lastName = this[Users.lastName] ?: ""

        return ReservationDTO(
            id = this[Reservations.id].value.toString(),
            userId = this[Reservations.userId].value.toString(),
            userName = "$firstName $lastName".trim().ifEmpty { null },
            userEmail = this[Users.email],
            blockId = this[Reservations.blockId]?.value?.toString(),
            date = this[Reservations.date].toString(),
            startTime = this[Reservations.startTime].toString(),
            endTime = this[Reservations.endTime].toString(),
            status = this[Reservations.status],
            creditsUsed = this[Reservations.creditsUsed],
            pricingItemId = this[Reservations.pricingItemId]?.value?.toString(),
            pricingItemName = this.getOrNull(PricingItems.nameCs),
            createdAt = this[Reservations.createdAt].toString(),
            cancelledAt = this[Reservations.cancelledAt]?.toString()
        )
    }
}

data class ReservationTimeSlot(
    val startTime: LocalTime,
    val endTime: LocalTime
)
