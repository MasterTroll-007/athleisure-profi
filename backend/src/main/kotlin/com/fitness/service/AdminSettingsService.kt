package com.fitness.service

import com.fitness.dto.AdminSettingsDTO
import com.fitness.dto.UpdateAdminSettingsRequest
import com.fitness.entity.User
import com.fitness.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.NoSuchElementException
import java.util.UUID

@Service
class AdminSettingsService(
    private val userRepository: UserRepository,
    private val slotService: SlotService
) {
    @Transactional
    fun updateSettings(adminId: UUID, request: UpdateAdminSettingsRequest): AdminSettingsDTO {
        val admin = userRepository.findById(adminId)
            .orElseThrow { NoSuchElementException("Admin not found") }

        val startHour = request.calendarStartHour ?: admin.calendarStartHour
        val endHour = request.calendarEndHour ?: admin.calendarEndHour

        if (startHour >= endHour) {
            throw IllegalArgumentException("Start hour must be less than end hour")
        }

        val calendarRangeSubmitted = request.calendarStartHour != null || request.calendarEndHour != null
        val saved = userRepository.save(
            admin.copy(
                calendarStartHour = startHour,
                calendarEndHour = endHour,
                adjacentBookingRequired = request.adjacentBookingRequired ?: admin.adjacentBookingRequired
            )
        )

        if (calendarRangeSubmitted) {
            slotService.deleteFutureSlotsOutsideCalendarRange(adminId, startHour, endHour)
        }

        return saved.toAdminSettingsDTO()
    }

    private fun User.toAdminSettingsDTO() = AdminSettingsDTO(
        calendarStartHour = calendarStartHour,
        calendarEndHour = calendarEndHour,
        inviteCode = inviteCode,
        inviteLink = inviteCode?.let { "/register/$it" },
        adjacentBookingRequired = adjacentBookingRequired
    )
}
