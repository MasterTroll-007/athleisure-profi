package com.fitness.app.data.api

import com.fitness.app.data.local.TokenManager
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthInterceptor @Inject constructor(
    private val tokenManager: TokenManager
) : Interceptor {

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

        // Skip auth header for public endpoints
        val isPublicEndpoint = publicPaths.any { originalRequest.url.encodedPath.contains(it) }

        if (isPublicEndpoint) {
            return chain.proceed(originalRequest)
        }

        // Use synchronous method - no runBlocking needed
        val token = tokenManager.getAccessTokenSync()

        val request = if (token != null) {
            originalRequest.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            originalRequest
        }

        // Let TokenAuthenticator handle 401/403 responses
        // Don't clear tokens here to avoid race conditions with the authenticator
        return chain.proceed(request)
    }
}
