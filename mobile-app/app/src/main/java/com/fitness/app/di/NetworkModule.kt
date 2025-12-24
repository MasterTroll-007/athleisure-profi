package com.fitness.app.di

import com.fitness.app.BuildConfig
import com.fitness.app.data.api.ApiService
import com.fitness.app.data.api.AuthInterceptor
import com.fitness.app.data.api.ConnectivityInterceptor
import com.fitness.app.data.api.TokenAuthenticator
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor {
        // Custom logger that redacts sensitive data
        val redactingLogger = HttpLoggingInterceptor.Logger { message ->
            // Only log in debug builds
            if (!BuildConfig.DEBUG) return@Logger

            var redacted = message

            // Redact authorization headers
            if (message.contains("Authorization:", ignoreCase = true)) {
                redacted = redacted.replace(
                    Regex("Authorization:\\s*Bearer\\s+[^\\s]+", RegexOption.IGNORE_CASE),
                    "Authorization: Bearer [REDACTED]"
                )
            }

            // Redact cookie headers
            if (message.startsWith("Cookie:", ignoreCase = true)) {
                redacted = "Cookie: [REDACTED]"
            }

            // Redact tokens in JSON bodies
            if (message.contains("\"accessToken\"") ||
                message.contains("\"refreshToken\"") ||
                message.contains("\"password\"")) {
                redacted = redacted
                    .replace(Regex("\"accessToken\"\\s*:\\s*\"[^\"]+\""), "\"accessToken\":\"[REDACTED]\"")
                    .replace(Regex("\"refreshToken\"\\s*:\\s*\"[^\"]+\""), "\"refreshToken\":\"[REDACTED]\"")
                    .replace(Regex("\"password\"\\s*:\\s*\"[^\"]+\""), "\"password\":\"[REDACTED]\"")
            }

            android.util.Log.d("OkHttp", redacted)
        }

        return HttpLoggingInterceptor(redactingLogger).apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        connectivityInterceptor: ConnectivityInterceptor,
        authInterceptor: AuthInterceptor,
        tokenAuthenticator: TokenAuthenticator,
        loggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient {
        // Runtime HTTPS validation for production builds
        if (!BuildConfig.DEBUG && !BuildConfig.API_BASE_URL.startsWith("https://")) {
            throw IllegalStateException("Production builds must use HTTPS API endpoint")
        }

        return OkHttpClient.Builder()
            .addInterceptor(connectivityInterceptor)
            .addInterceptor(authInterceptor)
            // HTTPS enforcement interceptor for production
            .addInterceptor { chain ->
                val request = chain.request()
                // In production, enforce HTTPS for all requests except localhost
                if (!BuildConfig.DEBUG &&
                    request.url.scheme != "https" &&
                    !request.url.host.startsWith("10.0.2.2") &&
                    request.url.host != "localhost") {
                    throw SecurityException("HTTPS required in production")
                }
                chain.proceed(request)
            }
            .authenticator(tokenAuthenticator)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        json: Json
    ): Retrofit {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL + "/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): ApiService {
        return retrofit.create(ApiService::class.java)
    }
}
