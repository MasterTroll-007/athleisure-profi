package com.fitness.app.data.repository

import com.fitness.app.data.api.ApiService
import com.fitness.app.data.api.NoConnectivityException
import com.fitness.app.data.dto.*
import com.fitness.app.data.local.TokenManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import okhttp3.ResponseBody
import javax.inject.Inject
import javax.inject.Singleton

sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String) : Result<Nothing>()
    object Loading : Result<Nothing>()
}

private val json = Json { ignoreUnknownKeys = true }

fun parseErrorBody(errorBody: ResponseBody?, fallback: String): String {
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

@Singleton
class AuthRepository @Inject constructor(
    private val apiService: ApiService,
    private val tokenManager: TokenManager
) {
    private val _currentUser = MutableStateFlow<UserDTO?>(null)
    val currentUser = _currentUser.asStateFlow()

    val isLoggedIn: Flow<Boolean> = tokenManager.isLoggedIn

    suspend fun login(email: String, password: String): Result<UserDTO> {
        return try {
            val response = apiService.login(LoginRequest(email, password))
            if (response.isSuccessful && response.body() != null) {
                val authResponse = response.body()!!
                tokenManager.saveTokens(authResponse.accessToken, authResponse.refreshToken)
                _currentUser.value = authResponse.user
                Result.Success(authResponse.user)
            } else {
                Result.Error(parseErrorBody(response.errorBody(), "Login failed"))
            }
        } catch (e: NoConnectivityException) {
            Result.Error("No internet connection")
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }

    suspend fun register(
        email: String,
        password: String,
        firstName: String?,
        lastName: String?,
        phone: String?
    ): Result<String> {
        return try {
            val response = apiService.register(
                RegisterRequest(email, password, firstName, lastName, phone)
            )
            if (response.isSuccessful) {
                Result.Success(response.body()?.message ?: "Registration successful")
            } else {
                Result.Error(parseErrorBody(response.errorBody(), "Registration failed"))
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }

    suspend fun verifyEmail(token: String): Result<String> {
        return try {
            val response = apiService.verifyEmail(VerifyEmailRequest(token))
            if (response.isSuccessful) {
                Result.Success(response.body()?.message ?: "Email verified")
            } else {
                Result.Error(parseErrorBody(response.errorBody(), "Verification failed"))
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }

    suspend fun resendVerification(email: String): Result<String> {
        return try {
            val response = apiService.resendVerification(ResendVerificationRequest(email))
            if (response.isSuccessful) {
                Result.Success(response.body()?.message ?: "Verification email sent")
            } else {
                Result.Error(parseErrorBody(response.errorBody(), "Failed to send verification email"))
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }

    suspend fun refreshToken(): Result<String> {
        return try {
            val refreshToken = tokenManager.getRefreshTokenSync() ?: return Result.Error("No refresh token")
            val response = apiService.refreshToken(RefreshTokenRequest(refreshToken))
            if (response.isSuccessful && response.body() != null) {
                tokenManager.updateAccessToken(response.body()!!.accessToken)
                Result.Success(response.body()!!.accessToken)
            } else {
                logout()
                Result.Error("Session expired")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }

    suspend fun logout() {
        try {
            apiService.logout()
        } catch (_: Exception) {
            // Ignore errors on logout
        }
        tokenManager.clearTokens()
        _currentUser.value = null
    }

    suspend fun getProfile(): Result<UserDTO> {
        return try {
            val response = apiService.getProfile()
            if (response.isSuccessful && response.body() != null) {
                _currentUser.value = response.body()
                Result.Success(response.body()!!)
            } else {
                Result.Error(parseErrorBody(response.errorBody(), "Failed to get profile"))
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }

    suspend fun updateProfile(
        firstName: String?,
        lastName: String?,
        phone: String?,
        locale: String?,
        theme: String?
    ): Result<UserDTO> {
        return try {
            val response = apiService.updateProfile(
                UpdateProfileRequest(firstName, lastName, phone, locale, theme)
            )
            if (response.isSuccessful && response.body() != null) {
                _currentUser.value = response.body()
                Result.Success(response.body()!!)
            } else {
                Result.Error(parseErrorBody(response.errorBody(), "Failed to update profile"))
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }

    suspend fun changePassword(currentPassword: String, newPassword: String): Result<String> {
        return try {
            val response = apiService.changePassword(
                ChangePasswordRequest(currentPassword, newPassword)
            )
            if (response.isSuccessful) {
                Result.Success(response.body()?.message ?: "Password changed")
            } else {
                Result.Error(parseErrorBody(response.errorBody(), "Failed to change password"))
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }
}
