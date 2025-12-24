package com.fitness.app.util

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import java.util.Locale

object LocaleHelper {

    /**
     * Wrap context with the specified locale
     * @param context Base context
     * @param languageCode Language code ("en", "cs") or null for system default
     * @return Context with applied locale
     */
    fun wrapContext(context: Context, languageCode: String?): Context {
        if (languageCode == null) {
            // Use system default
            return context
        }

        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocales(LocaleList(locale))
        } else {
            @Suppress("DEPRECATION")
            config.locale = locale
        }
        config.setLocale(locale)

        return context.createConfigurationContext(config)
    }

    /**
     * Get display name for a language code
     */
    fun getLanguageDisplayName(languageCode: String?): String {
        return when (languageCode) {
            "en" -> "English"
            "cs" -> "Čeština"
            else -> "System"
        }
    }

    /**
     * Get all available language options
     */
    fun getAvailableLanguages(): List<LanguageOption> {
        return listOf(
            LanguageOption(null, "System default", "Dle systému"),
            LanguageOption("en", "English", "English"),
            LanguageOption("cs", "Čeština", "Čeština")
        )
    }
}

data class LanguageOption(
    val code: String?,
    val nameEn: String,
    val nameCs: String
)
