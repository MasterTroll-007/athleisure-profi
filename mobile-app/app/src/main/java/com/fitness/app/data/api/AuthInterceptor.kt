package com.fitness.app.data.api

import com.fitness.app.data.local.TokenManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthInterceptor @Inject constructor(
    private val tokenManager: TokenManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // Skip auth header for public endpoints
        val publicPaths = listOf(
            "/auth/login",
            "/auth/register",
            "/auth/verify-email",
            "/auth/resend-verification",
            "/auth/refresh",
            "/plans",
            "/credits/packages"
        )

        val isPublicEndpoint = publicPaths.any { originalRequest.url.encodedPath.contains(it) }

        if (isPublicEndpoint) {
            return chain.proceed(originalRequest)
        }

        val token = runBlocking {
            tokenManager.accessToken.first()
        }

        val request = if (token != null) {
            originalRequest.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            originalRequest
        }

        return chain.proceed(request)
    }
}
