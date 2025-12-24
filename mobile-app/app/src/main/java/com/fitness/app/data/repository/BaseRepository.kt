package com.fitness.app.data.repository

import com.fitness.app.data.api.NoConnectivityException
import com.fitness.app.data.dto.ErrorResponse
import kotlinx.serialization.json.Json
import okhttp3.ResponseBody
import retrofit2.HttpException
import retrofit2.Response
import java.net.SocketTimeoutException
import java.net.UnknownHostException

private val json = Json { ignoreUnknownKeys = true }

/**
 * Base class for repositories providing consistent error handling patterns.
 */
abstract class BaseRepository {

    /**
     * Safely execute an API call with consistent error handling.
     * Handles connectivity issues, HTTP errors, and parsing failures.
     */
    protected suspend fun <T> safeApiCall(
        fallbackError: String = "An error occurred",
        apiCall: suspend () -> Response<T>
    ): Result<T> {
        return try {
            val response = apiCall()
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    Result.Success(body)
                } else {
                    Result.Error(fallbackError)
                }
            } else {
                Result.Error(parseErrorBody(response.errorBody(), fallbackError))
            }
        } catch (e: NoConnectivityException) {
            Result.Error("No internet connection")
        } catch (e: SocketTimeoutException) {
            Result.Error("Connection timed out")
        } catch (e: UnknownHostException) {
            Result.Error("Unable to reach server")
        } catch (e: HttpException) {
            Result.Error("Server error: ${e.code()}")
        } catch (e: Exception) {
            Result.Error(e.message ?: fallbackError)
        }
    }

    /**
     * Safely execute an API call that returns a message response.
     */
    protected suspend fun safeApiCallForMessage(
        fallbackError: String = "An error occurred",
        successMessage: String = "Success",
        apiCall: suspend () -> Response<*>
    ): Result<String> {
        return try {
            val response = apiCall()
            if (response.isSuccessful) {
                Result.Success(successMessage)
            } else {
                Result.Error(parseErrorBody(response.errorBody(), fallbackError))
            }
        } catch (e: NoConnectivityException) {
            Result.Error("No internet connection")
        } catch (e: SocketTimeoutException) {
            Result.Error("Connection timed out")
        } catch (e: UnknownHostException) {
            Result.Error("Unable to reach server")
        } catch (e: Exception) {
            Result.Error(e.message ?: fallbackError)
        }
    }

    protected fun parseErrorBody(errorBody: ResponseBody?, fallback: String): String {
        return try {
            errorBody?.use { body ->
                val errorString = body.string()
                if (errorString.isBlank()) return fallback
                val errorResponse = json.decodeFromString<ErrorResponse>(errorString)
                errorResponse.error ?: errorResponse.message ?: fallback
            } ?: fallback
        } catch (e: Exception) {
            fallback
        }
    }
}
