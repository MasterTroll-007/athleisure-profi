package com.fitness.app.data.api

import android.util.Log
import com.fitness.app.BuildConfig
import com.fitness.app.data.dto.RefreshTokenRequest
import com.fitness.app.data.dto.RefreshTokenResponse
import com.fitness.app.data.local.TokenManager
import kotlinx.serialization.json.Json
import okhttp3.Authenticator
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OkHttp Authenticator that handles 401 responses by:
 * 1. Attempting to refresh the access token using the refresh token
 * 2. Retrying the original request with the new access token
 * 3. Clearing tokens if refresh fails (triggers logout)
 *
 * Thread-safety: Uses synchronized block with isRefreshing flag
 * to prevent duplicate refresh attempts from concurrent requests.
 */
@Singleton
class TokenAuthenticator @Inject constructor(
    private val tokenManager: TokenManager,
    private val json: Json
) : Authenticator {

    companion object {
        private const val TAG = "TokenAuthenticator"
        private const val REFRESH_WAIT_TIME_MS = 100L
        private const val MAX_WAIT_ITERATIONS = 50 // max 5 seconds wait
    }

    // Refresh client with proper timeouts
    private val refreshClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .callTimeout(15, TimeUnit.SECONDS)
        .retryOnConnectionFailure(false)
        .build()

    private val lock = Any()
    @Volatile
    private var isRefreshing = false

    override fun authenticate(route: Route?, response: Response): Request? {
        // Don't retry if we've already tried to refresh
        if (response.request.header("X-Retry-After-Refresh") != null) {
            return null
        }

        // Don't try to refresh for auth endpoints
        val path = response.request.url.encodedPath
        if (path.contains("/auth/login") ||
            path.contains("/auth/register") ||
            path.contains("/auth/refresh")) {
            return null
        }

        synchronized(lock) {
            // Wait if another thread is currently refreshing
            var waitCount = 0
            while (isRefreshing && waitCount < MAX_WAIT_ITERATIONS) {
                try {
                    Thread.sleep(REFRESH_WAIT_TIME_MS)
                    waitCount++
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                    return null
                }
            }

            // Check if another thread has already refreshed the token
            val currentToken = tokenManager.getAccessTokenSync()
            val requestToken = response.request.header("Authorization")?.removePrefix("Bearer ")

            // If tokens are different, another thread already refreshed - retry with new token
            if (currentToken != null && currentToken != requestToken) {
                return response.request.newBuilder()
                    .header("Authorization", "Bearer $currentToken")
                    .header("X-Retry-After-Refresh", "true")
                    .build()
            }

            // Try to refresh the token
            val refreshToken = tokenManager.getRefreshTokenSync() ?: run {
                tokenManager.clearTokens()
                return null
            }

            isRefreshing = true
            try {
                val tokenResponse = refreshTokens(refreshToken)

                if (tokenResponse != null) {
                    // Save BOTH tokens - backend rotates refresh token on each refresh
                    tokenManager.saveTokens(tokenResponse.accessToken, tokenResponse.refreshToken)
                    return response.request.newBuilder()
                        .header("Authorization", "Bearer ${tokenResponse.accessToken}")
                        .header("X-Retry-After-Refresh", "true")
                        .build()
                } else {
                    // Refresh failed - clear tokens to trigger logout
                    Log.e(TAG, "Token refresh failed")
                    tokenManager.clearTokens()
                    return null
                }
            } finally {
                isRefreshing = false
            }
        }
    }

    private fun refreshTokens(refreshToken: String): RefreshTokenResponse? {
        return try {
            val requestBody = json.encodeToString(
                RefreshTokenRequest.serializer(),
                RefreshTokenRequest(refreshToken)
            ).toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url(BuildConfig.API_BASE_URL + "/auth/refresh")
                .post(requestBody)
                .build()

            val response = refreshClient.newCall(request).execute()

            if (response.isSuccessful) {
                response.body?.string()?.let { body ->
                    json.decodeFromString(RefreshTokenResponse.serializer(), body)
                }
            } else {
                Log.e(TAG, "Token refresh failed with code: ${response.code}")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Token refresh error", e)
            null
        }
    }
}
