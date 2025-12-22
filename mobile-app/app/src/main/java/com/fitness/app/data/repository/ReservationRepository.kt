package com.fitness.app.data.repository

import com.fitness.app.data.api.ApiService
import com.fitness.app.data.dto.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReservationRepository @Inject constructor(
    private val apiService: ApiService
) {
    suspend fun getReservations(): Result<List<ReservationDTO>> {
        return try {
            val response = apiService.getReservations()
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!)
            } else {
                Result.Error(response.errorBody()?.string() ?: "Failed to get reservations")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }

    suspend fun getUpcomingReservations(): Result<List<ReservationDTO>> {
        return try {
            val response = apiService.getUpcomingReservations()
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!)
            } else {
                Result.Error(response.errorBody()?.string() ?: "Failed to get upcoming reservations")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }

    suspend fun getAvailableSlots(start: String, end: String): Result<List<AvailableSlotDTO>> {
        return try {
            val response = apiService.getAvailableSlots(start, end)
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!)
            } else {
                Result.Error(response.errorBody()?.string() ?: "Failed to get available slots")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }

    suspend fun createReservation(slotId: String, notes: String?): Result<ReservationDTO> {
        return try {
            val response = apiService.createReservation(CreateReservationRequest(slotId, notes))
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!)
            } else {
                Result.Error(response.errorBody()?.string() ?: "Failed to create reservation")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }

    suspend fun cancelReservation(id: String): Result<String> {
        return try {
            val response = apiService.cancelReservation(id)
            if (response.isSuccessful) {
                Result.Success(response.body()?.message ?: "Reservation cancelled")
            } else {
                Result.Error(response.errorBody()?.string() ?: "Failed to cancel reservation")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }
}
