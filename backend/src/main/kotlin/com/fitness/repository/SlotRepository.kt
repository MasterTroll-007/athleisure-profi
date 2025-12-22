package com.fitness.repository

import com.fitness.entity.Slot
import com.fitness.entity.SlotStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.LocalTime
import java.util.*

@Repository
interface SlotRepository : JpaRepository<Slot, UUID> {

    fun findByDateBetween(startDate: LocalDate, endDate: LocalDate): List<Slot>

    fun findByDateBetweenAndStatus(startDate: LocalDate, endDate: LocalDate, status: SlotStatus): List<Slot>

    fun findByDateBetweenAndStatusIn(startDate: LocalDate, endDate: LocalDate, statuses: List<SlotStatus>): List<Slot>

    fun findByDateAndStartTime(date: LocalDate, startTime: LocalTime): Slot?

    fun findByDate(date: LocalDate): List<Slot>

    @Query("SELECT s FROM Slot s WHERE s.date BETWEEN :startDate AND :endDate AND s.status IN ('UNLOCKED', 'RESERVED')")
    fun findUserVisibleSlots(startDate: LocalDate, endDate: LocalDate): List<Slot>

    @Modifying
    @Query("UPDATE Slot s SET s.status = :newStatus WHERE s.date BETWEEN :startDate AND :endDate AND s.status = :currentStatus")
    fun updateStatusByDateRangeAndStatus(startDate: LocalDate, endDate: LocalDate, currentStatus: SlotStatus, newStatus: SlotStatus): Int

    fun countByDateBetweenAndStatus(startDate: LocalDate, endDate: LocalDate, status: SlotStatus): Long

    fun existsByDateAndStartTime(date: LocalDate, startTime: LocalTime): Boolean
}
