package com.fitness.app.data.api

import android.util.Log
import com.fitness.app.data.local.TokenManager
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthInterceptor @Inject constructor(
    private val tokenManager: TokenManager
) : Interceptor {

    companion object {
        private const val TAG = "AuthInterceptor"
    }

    private val publicPaths = listOf(
        "/auth/login",
        "/auth/register",
        "/auth/verify-email",
        "/auth/resend-verification",
        "/auth/refresh",
        "/plans",
        "/credits/packages"
    )

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val path = originalRequest.url.encodedPath

        // Skip auth header for public endpoints
        val isPublicEndpoint = publicPaths.any { path.contains(it) }

        if (isPublicEndpoint) {
            return chain.proceed(originalRequest)
        }

        // Use synchronous method - no runBlocking needed
        val token = tokenManager.getAccessTokenSync()

        // If no token and trying to access protected endpoint, clear and force re-login
        if (token == null) {
            Log.w(TAG, "No token available for protected endpoint: $path")
            tokenManager.clearTokens()
        }

        val request = if (token != null) {
            originalRequest.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            originalRequest
        }

        val response = chain.proceed(request)

        // Handle 401 (Unauthorized) and 403 (Forbidden) - clear tokens to force re-login
        // TokenAuthenticator handles 401 with retry, but 403 needs explicit handling
        if (response.code == 403) {
            Log.w(TAG, "403 Forbidden on $path - clearing tokens")
            tokenManager.clearTokens()
        }

        return response
    }
}
