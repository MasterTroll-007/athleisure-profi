package com.fitness.app.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
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
        private const val PREFS_NAME = "locale_prefs"
        private const val KEY_LOCALE = "selected_locale"
    }

    // SharedPreferences for synchronous access in attachBaseContext
    private val sharedPrefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

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
