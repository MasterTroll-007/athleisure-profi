package com.fitness.repository

import com.fitness.entity.AvailabilityBlock
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.*

@Repository
interface AvailabilityBlockRepository : JpaRepository<AvailabilityBlock, UUID> {
    @Query("SELECT a FROM AvailabilityBlock a WHERE a.isActive = true")
    fun findByIsActiveTrue(): List<AvailabilityBlock>

    @Query("SELECT a FROM AvailabilityBlock a WHERE a.isActive = true AND a.adminId = :adminId")
    fun findByIsActiveTrueAndAdminId(adminId: UUID): List<AvailabilityBlock>

    fun findByAdminId(adminId: UUID): List<AvailabilityBlock>
    
    @Query("SELECT a FROM AvailabilityBlock a WHERE a.dayOfWeek = :dayOfWeek AND a.isRecurring = true AND (a.isBlocked IS NULL OR a.isBlocked = false)")
    fun findByDayOfWeekAndIsRecurringTrueAndIsBlockedFalse(dayOfWeek: DayOfWeek): List<AvailabilityBlock>
    
    @Query("SELECT a FROM AvailabilityBlock a WHERE a.specificDate = :specificDate AND (a.isBlocked IS NULL OR a.isBlocked = false)")
    fun findBySpecificDateAndIsBlockedFalse(specificDate: LocalDate): List<AvailabilityBlock>
    
    @Query("SELECT a FROM AvailabilityBlock a WHERE a.specificDate = :specificDate AND a.isBlocked = true")
    fun findBySpecificDateAndIsBlockedTrue(specificDate: LocalDate): List<AvailabilityBlock>
    
    @Query("SELECT a FROM AvailabilityBlock a WHERE a.isRecurring = true AND a.isActive = true ORDER BY a.dayOfWeek, a.startTime")
    fun findAllRecurring(): List<AvailabilityBlock>
}
