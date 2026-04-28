package com.fitness.repository

import com.fitness.entity.Reservation
import jakarta.persistence.LockModeType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.util.*

@Repository
interface ReservationRepository : JpaRepository<Reservation, UUID> {
    fun findByUserId(userId: UUID): List<Reservation>
    fun findByUserIdOrderByDateDescStartTimeDesc(userId: UUID, pageable: Pageable): Page<Reservation>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Reservation r WHERE r.id = :id")
    fun findByIdForUpdate(id: UUID): Reservation?
    
    @Query("SELECT r FROM Reservation r WHERE r.userId = :userId AND r.date >= :date AND r.status = 'confirmed' ORDER BY r.date, r.startTime")
    fun findUpcomingByUserId(userId: UUID, date: LocalDate): List<Reservation>

    @Query(
        value = """
            SELECT r FROM Reservation r
            WHERE r.userId = :userId
            AND r.status = 'confirmed'
            AND (r.date > :date OR (r.date = :date AND r.startTime >= :time))
            ORDER BY r.date ASC, r.startTime ASC
        """,
        countQuery = """
            SELECT COUNT(r) FROM Reservation r
            WHERE r.userId = :userId
            AND r.status = 'confirmed'
            AND (r.date > :date OR (r.date = :date AND r.startTime >= :time))
        """
    )
    fun findUpcomingByUserId(userId: UUID, date: LocalDate, time: java.time.LocalTime, pageable: Pageable): Page<Reservation>

    @Query(
        value = """
            SELECT r FROM Reservation r
            WHERE r.userId = :userId
            AND r.status <> 'cancelled'
            AND (r.date < :date OR (r.date = :date AND r.startTime < :time))
            ORDER BY r.date DESC, r.startTime DESC
        """,
        countQuery = """
            SELECT COUNT(r) FROM Reservation r
            WHERE r.userId = :userId
            AND r.status <> 'cancelled'
            AND (r.date < :date OR (r.date = :date AND r.startTime < :time))
        """
    )
    fun findPastByUserId(userId: UUID, date: LocalDate, time: java.time.LocalTime, pageable: Pageable): Page<Reservation>
    
    fun findByDate(date: LocalDate): List<Reservation>

    @Query("""
        SELECT r FROM Reservation r
        WHERE r.date = :date
        AND r.status = 'confirmed'
        AND r.slotId IN :slotIds
        ORDER BY r.startTime
    """)
    fun findConfirmedByDateAndSlotIdIn(date: LocalDate, slotIds: Collection<UUID>): List<Reservation>
    
    @Query("SELECT r FROM Reservation r WHERE r.date BETWEEN :startDate AND :endDate")
    fun findByDateRange(startDate: LocalDate, endDate: LocalDate): List<Reservation>

    @Query("""
        SELECT r FROM Reservation r
        WHERE r.date BETWEEN :startDate AND :endDate
        AND (
            r.slotId IN (SELECT s.id FROM Slot s WHERE s.adminId = :adminId)
            OR r.userId IN (SELECT u.id FROM User u WHERE u.trainerId = :adminId)
        )
    """)
    fun findByDateRangeForAdmin(startDate: LocalDate, endDate: LocalDate, adminId: UUID): List<Reservation>
    
    @Query("SELECT r FROM Reservation r WHERE r.date = :date AND r.blockId = :blockId AND r.status = 'confirmed'")
    fun findByDateAndBlockId(date: LocalDate, blockId: UUID): List<Reservation>

    @Query("SELECT r FROM Reservation r WHERE r.date = :date AND r.slotId = :slotId AND r.status = 'confirmed'")
    fun findByDateAndSlotId(date: LocalDate, slotId: UUID): List<Reservation>

    @Query("SELECT COUNT(r) FROM Reservation r WHERE r.date = :date AND r.slotId = :slotId AND r.status = 'confirmed'")
    fun countConfirmedByDateAndSlotId(date: LocalDate, slotId: UUID): Long

    @Query("SELECT COUNT(r) FROM Reservation r WHERE r.date BETWEEN :startDate AND :endDate AND r.status = 'confirmed'")
    fun countByDateRange(startDate: LocalDate, endDate: LocalDate): Long

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

    @Query("SELECT COUNT(r) FROM Reservation r WHERE r.status = :status AND r.date BETWEEN :startDate AND :endDate")
    fun countByStatusAndDateBetween(status: String, startDate: LocalDate, endDate: LocalDate): Long

    @Query("SELECT COUNT(r) FROM Reservation r WHERE r.userId = :userId AND r.status = :status")
    fun countByUserIdAndStatus(userId: UUID, status: String): Long

    @Query("SELECT r FROM Reservation r WHERE r.slotId = :slotId AND r.status = 'confirmed'")
    fun findConfirmedBySlotId(slotId: UUID): List<Reservation>
}
