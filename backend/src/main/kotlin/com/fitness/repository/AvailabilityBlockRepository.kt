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
    fun findByDayOfWeekAndIsRecurringTrueAndIsBlockedFalse(dayOfWeek: DayOfWeek): List<AvailabilityBlock>
    fun findBySpecificDateAndIsBlockedFalse(specificDate: LocalDate): List<AvailabilityBlock>
    fun findBySpecificDateAndIsBlockedTrue(specificDate: LocalDate): List<AvailabilityBlock>
    
    @Query("SELECT a FROM AvailabilityBlock a WHERE a.isRecurring = true ORDER BY a.dayOfWeek, a.startTime")
    fun findAllRecurring(): List<AvailabilityBlock>
}
