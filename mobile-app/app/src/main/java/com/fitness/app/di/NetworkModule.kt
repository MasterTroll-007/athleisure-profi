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
import okhttp3.CertificatePinner
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

    /**
     * Certificate pinning for production builds to prevent MITM attacks.
     *
     * To get the certificate pin for your domain, run:
     * openssl s_client -servername domi-fit.online -connect domi-fit.online:443 | \
     *   openssl x509 -pubkey -noout | \
     *   openssl pkey -pubin -outform der | \
     *   openssl dgst -sha256 -binary | openssl enc -base64
     *
     * Or use: https://www.ssllabs.com/ssltest/ to get the pin from the certificate chain
     *
     * Add multiple pins (current + backup) to handle certificate rotation.
     */
    @Provides
    @Singleton
    fun provideCertificatePinner(): CertificatePinner? {
        // Only enable certificate pinning in production builds
        if (BuildConfig.DEBUG) {
            return null
        }

        return CertificatePinner.Builder()
            // Primary pin for domi-fit.online
            // TODO: Replace with actual SHA-256 pin from your certificate
            // Get pin using: openssl s_client -servername domi-fit.online -connect domi-fit.online:443 2>/dev/null | openssl x509 -pubkey -noout | openssl pkey -pubin -outform der 2>/dev/null | openssl dgst -sha256 -binary | openssl enc -base64
            .add("domi-fit.online", "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
            // Backup pin (e.g., Let's Encrypt intermediate or your CA's root)
            // ISRG Root X1 - Let's Encrypt's root certificate
            .add("domi-fit.online", "sha256/C5+lpZ7tcVwmwQIMcRtPbsQtWLABXhQzejna0wHFr8M=")
            .build()
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        connectivityInterceptor: ConnectivityInterceptor,
        authInterceptor: AuthInterceptor,
        tokenAuthenticator: TokenAuthenticator,
        loggingInterceptor: HttpLoggingInterceptor,
        certificatePinner: CertificatePinner?
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
            // Apply certificate pinning for production builds
            .apply {
                certificatePinner?.let { certificatePinner(it) }
            }
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
