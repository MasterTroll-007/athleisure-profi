package com.fitness.app.data.repository

import com.fitness.app.data.api.ApiService
import com.fitness.app.data.dto.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReservationRepository @Inject constructor(
    private val apiService: ApiService
) : BaseRepository() {

    suspend fun getReservations(): Result<List<ReservationDTO>> {
        return safeApiCall("Failed to get reservations") {
            apiService.getReservations()
        }
    }

    suspend fun getUpcomingReservations(): Result<List<ReservationDTO>> {
        return safeApiCall("Failed to get upcoming reservations") {
            apiService.getUpcomingReservations()
        }
    }

    suspend fun getAvailableSlots(start: String, end: String): Result<List<AvailableSlotDTO>> {
        return try {
            val response = apiService.getAvailableSlots(start, end)
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!.slots)
            } else {
                Result.Error(parseErrorBody(response.errorBody(), "Failed to get available slots"))
            }
        } catch (e: com.fitness.app.data.api.NoConnectivityException) {
            Result.Error("No internet connection")
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }

    suspend fun createReservation(slot: AvailableSlotDTO): Result<ReservationDTO> {
        return safeApiCall("Failed to create reservation") {
            apiService.createReservation(
                CreateReservationRequest(
                    date = slot.date,
                    startTime = slot.startTime,
                    endTime = slot.endTime,
                    blockId = slot.id
                )
            )
        }
    }

    suspend fun cancelReservation(id: String): Result<String> {
        return safeApiCallForMessage(
            fallbackError = "Failed to cancel reservation",
            successMessage = "Reservation cancelled"
        ) {
            apiService.cancelReservation(id)
        }
    }
}
