package com.fitness.service

import com.fitness.repository.PricingItemRepository
import com.fitness.repository.TrainingLocationRepository
import com.fitness.repository.UserRepository
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import java.time.LocalTime
import java.util.UUID

@Service
class SlotPolicyService(
    private val userRepository: UserRepository,
    private val locationRepository: TrainingLocationRepository,
    private val pricingItemRepository: PricingItemRepository
) {
    fun requireClientOwnedByAdmin(userId: UUID?, adminId: UUID) {
        if (userId == null) return
        val user = userRepository.findById(userId).orElseThrow { IllegalArgumentException("Assigned user not found") }
        if (user.trainerId != adminId) {
            throw AccessDeniedException("Assigned user does not belong to this trainer")
        }
    }

    fun requireLocationOwnedByAdmin(locationId: UUID?, adminId: UUID) {
        if (locationId == null) return
        val location = locationRepository.findById(locationId)
            .orElseThrow { IllegalArgumentException("Training location not found") }
        if (location.adminId != adminId) {
            throw AccessDeniedException("Training location does not belong to this trainer")
        }
    }

    fun requirePricingItemsOwnedByAdmin(pricingItemIds: List<String>, adminId: UUID) {
        val ids = pricingItemIds.distinct().map { UUID.fromString(it) }
        if (ids.isEmpty()) return
        val items = pricingItemRepository.findAllById(ids)
        if (items.size != ids.size) {
            throw IllegalArgumentException("Pricing item not found")
        }
        if (items.any { it.adminId != adminId }) {
            throw AccessDeniedException("Pricing item does not belong to this trainer")
        }
    }

    fun isWithinCalendarHours(
        startTime: LocalTime,
        endTime: LocalTime,
        calendarStartHour: Int,
        calendarEndHour: Int
    ): Boolean {
        val startBoundary = LocalTime.of(calendarStartHour, 0)
        val endBoundary = if (calendarEndHour >= 24) LocalTime.MAX else LocalTime.of(calendarEndHour, 0)
        return !startTime.isBefore(startBoundary) && !endTime.isAfter(endBoundary)
    }

    fun requireWithinAdminCalendarHours(adminId: UUID, startTime: LocalTime, endTime: LocalTime) {
        val admin = userRepository.findById(adminId)
            .orElseThrow { IllegalArgumentException("Admin not found") }

        if (!isWithinCalendarHours(startTime, endTime, admin.calendarStartHour, admin.calendarEndHour)) {
            val start = admin.calendarStartHour.toString().padStart(2, '0')
            val end = admin.calendarEndHour.toString().padStart(2, '0')
            throw IllegalArgumentException("Slot must be within calendar hours $start:00-$end:00")
        }
    }
}
