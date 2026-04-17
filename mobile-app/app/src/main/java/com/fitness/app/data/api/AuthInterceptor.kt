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

    // List of suffix-match public endpoints. We match on `endsWith` (after
    // stripping any `/api` prefix) so that e.g. "/api/admin/plans" is NOT
    // mistakenly classified as public just because it contains "/plans".
    private val publicPaths = listOf(
        "/auth/login",
        "/auth/register",
        "/auth/verify-email",
        "/auth/resend-verification",
        "/auth/refresh",
        "/plans"
    )

    private fun isPublicPath(path: String): Boolean {
        // Normalise by stripping the `/api` prefix if present.
        val p = path.removePrefix("/api")
        if (p.startsWith("/auth/trainer/")) return true
        return publicPaths.any { p == it || p.startsWith("$it/") }
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val path = originalRequest.url.encodedPath

        // Skip auth header for public endpoints
        val isPublicEndpoint = isPublicPath(path)

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
