package com.fitness.app.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val PREFS_NAME = "auth_prefs_encrypted"
        private const val ACCESS_TOKEN_KEY = "access_token"
        private const val REFRESH_TOKEN_KEY = "refresh_token"
    }

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val encryptedPrefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    // StateFlow for reactive updates
    private val _accessToken = MutableStateFlow<String?>(encryptedPrefs.getString(ACCESS_TOKEN_KEY, null))
    private val _refreshToken = MutableStateFlow<String?>(encryptedPrefs.getString(REFRESH_TOKEN_KEY, null))

    // Event flow for forced logout (when token refresh fails)
    private val _logoutEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val logoutEvent = _logoutEvent.asSharedFlow()

    val accessToken: Flow<String?> = _accessToken
    val refreshToken: Flow<String?> = _refreshToken
    val isLoggedIn: Flow<Boolean> = _accessToken.map { it != null }

    private val lock = Any()

    fun saveTokens(accessToken: String, refreshToken: String) {
        synchronized(lock) {
            encryptedPrefs.edit()
                .putString(ACCESS_TOKEN_KEY, accessToken)
                .putString(REFRESH_TOKEN_KEY, refreshToken)
                .commit() // Use commit() for synchronous update to prevent race conditions
            _accessToken.value = accessToken
            _refreshToken.value = refreshToken
        }
    }

    fun updateAccessToken(accessToken: String) {
        synchronized(lock) {
            encryptedPrefs.edit()
                .putString(ACCESS_TOKEN_KEY, accessToken)
                .commit() // Use commit() for synchronous update
            _accessToken.value = accessToken
        }
    }

    fun clearTokens() {
        synchronized(lock) {
            val hadTokens = _accessToken.value != null
            encryptedPrefs.edit()
                .remove(ACCESS_TOKEN_KEY)
                .remove(REFRESH_TOKEN_KEY)
                .commit() // Use commit() for synchronous update
            _accessToken.value = null
            _refreshToken.value = null
            // Emit logout event if we actually had tokens (session expired)
            if (hadTokens) {
                _logoutEvent.tryEmit(Unit)
            }
        }
    }

    fun getAccessTokenSync(): String? {
        return encryptedPrefs.getString(ACCESS_TOKEN_KEY, null)
    }

    fun getRefreshTokenSync(): String? {
        return encryptedPrefs.getString(REFRESH_TOKEN_KEY, null)
    }
}
