package com.fitness.app.data.api

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
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OkHttp Authenticator that handles 401 responses by:
 * 1. Attempting to refresh the access token using the refresh token
 * 2. Retrying the original request with the new access token
 * 3. Clearing tokens if refresh fails (triggers logout)
 */
@Singleton
class TokenAuthenticator @Inject constructor(
    private val tokenManager: TokenManager,
    private val json: Json
) : Authenticator {

    private val refreshClient = OkHttpClient.Builder().build()
    private val lock = Any()

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
                tokenManager.clearTokens()
                return null
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
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}
