package com.fitness.app.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val LOCALE_KEY = stringPreferencesKey("selected_locale")
        private val BIOMETRIC_ENABLED_KEY = booleanPreferencesKey("biometric_login_enabled")
        private const val PREFS_NAME = "locale_prefs"
        private const val KEY_LOCALE = "selected_locale"
        private const val ENCRYPTED_PREFS_NAME = "biometric_credentials"
        private const val KEY_BIOMETRIC_EMAIL = "biometric_email"
        private const val KEY_BIOMETRIC_PASSWORD = "biometric_password"
    }

    // SharedPreferences for synchronous access in attachBaseContext
    private val sharedPrefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // Encrypted SharedPreferences for biometric credentials
    private val encryptedPrefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            ENCRYPTED_PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    /**
     * Flow for observing locale changes (for reactive UI updates)
     */
    val selectedLocale: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[LOCALE_KEY]
    }

    /**
     * Synchronous getter for use in attachBaseContext (before Hilt injection)
     */
    fun getSelectedLocaleSync(): String? {
        return sharedPrefs.getString(KEY_LOCALE, null)
    }

    /**
     * Set the selected locale
     * @param locale Language code ("en", "cs") or null for system default
     */
    suspend fun setLocale(locale: String?) {
        // Save to DataStore (async, for Flow)
        context.dataStore.edit { preferences ->
            if (locale != null) {
                preferences[LOCALE_KEY] = locale
            } else {
                preferences.remove(LOCALE_KEY)
            }
        }
        // Also save to SharedPreferences (for sync access in attachBaseContext)
        sharedPrefs.edit().apply {
            if (locale != null) {
                putString(KEY_LOCALE, locale)
            } else {
                remove(KEY_LOCALE)
            }
            apply()
        }
    }

    /**
     * Check if biometric login is enabled
     */
    suspend fun isBiometricLoginEnabled(): Boolean {
        return context.dataStore.data.map { preferences ->
            preferences[BIOMETRIC_ENABLED_KEY] ?: false
        }.first()
    }

    /**
     * Enable or disable biometric login
     */
    suspend fun setBiometricLoginEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[BIOMETRIC_ENABLED_KEY] = enabled
        }
    }

    /**
     * Save encrypted biometric credentials
     */
    suspend fun saveBiometricCredentials(email: String, password: String) {
        encryptedPrefs.edit().apply {
            putString(KEY_BIOMETRIC_EMAIL, email)
            putString(KEY_BIOMETRIC_PASSWORD, password)
            apply()
        }
    }

    /**
     * Get saved biometric credentials (email and password)
     */
    suspend fun getBiometricCredentials(): Pair<String, String>? {
        val email = encryptedPrefs.getString(KEY_BIOMETRIC_EMAIL, null)
        val password = encryptedPrefs.getString(KEY_BIOMETRIC_PASSWORD, null)
        return if (email != null && password != null) {
            Pair(email, password)
        } else {
            null
        }
    }

    /**
     * Clear saved biometric credentials
     */
    suspend fun clearBiometricCredentials() {
        encryptedPrefs.edit().apply {
            remove(KEY_BIOMETRIC_EMAIL)
            remove(KEY_BIOMETRIC_PASSWORD)
            apply()
        }
    }
}

/**
 * Standalone function for reading locale before Hilt injection
 */
object LocalePreferences {
    private const val PREFS_NAME = "locale_prefs"
    private const val KEY_LOCALE = "selected_locale"

    fun getSelectedLocale(context: Context): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LOCALE, null)
    }
}
