package com.fitness.repository

import com.fitness.entity.Reservation
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.util.*

@Repository
interface ReservationRepository : JpaRepository<Reservation, UUID> {
    fun findByUserId(userId: UUID): List<Reservation>
    
    @Query("SELECT r FROM Reservation r WHERE r.userId = :userId AND r.date >= :date AND r.status = 'confirmed' ORDER BY r.date, r.startTime")
    fun findUpcomingByUserId(userId: UUID, date: LocalDate): List<Reservation>
    
    fun findByDate(date: LocalDate): List<Reservation>
    
    @Query("SELECT r FROM Reservation r WHERE r.date BETWEEN :startDate AND :endDate")
    fun findByDateRange(startDate: LocalDate, endDate: LocalDate): List<Reservation>
    
    @Query("SELECT r FROM Reservation r WHERE r.date = :date AND r.blockId = :blockId AND r.status = 'confirmed'")
    fun findByDateAndBlockId(date: LocalDate, blockId: UUID): List<Reservation>

    @Query("SELECT r FROM Reservation r WHERE r.date = :date AND r.slotId = :slotId AND r.status = 'confirmed'")
    fun findByDateAndSlotId(date: LocalDate, slotId: UUID): List<Reservation>

    @Query("SELECT COUNT(r) FROM Reservation r WHERE r.date BETWEEN :startDate AND :endDate AND r.status = 'confirmed'")
    fun countByDateRange(startDate: LocalDate, endDate: LocalDate): Long

    @Query("SELECT COUNT(r) > 0 FROM Reservation r WHERE r.userId = :userId AND r.date = :date AND r.status = 'confirmed'")
    fun existsByUserIdAndDateConfirmed(userId: UUID, date: LocalDate): Boolean

    @Query("""
        SELECT r FROM Reservation r
        WHERE r.date = :date
        AND r.status = 'confirmed'
        ORDER BY r.startTime
    """)
    fun findConfirmedByDate(date: LocalDate): List<Reservation>

    @Query("""
        SELECT r FROM Reservation r
        WHERE r.date BETWEEN :startDate AND :endDate
        AND r.status = 'confirmed'
        ORDER BY r.date, r.startTime
    """)
    fun findConfirmedByDateRange(startDate: LocalDate, endDate: LocalDate): List<Reservation>
}
