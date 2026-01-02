package com.fitness.app.data.local

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.fitness.app.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BiometricAuthManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesManager: PreferencesManager
) {
    private val biometricManager = BiometricManager.from(context)

    /**
     * Check if biometric authentication is available on this device
     */
    fun isBiometricAvailable(): Boolean {
        return when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> true
            else -> false
        }
    }

    /**
     * Check if biometric authentication is enrolled (user has set up fingerprint/face)
     */
    fun isBiometricEnrolled(): Boolean {
        return when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> true
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> false
            else -> false
        }
    }

    /**
     * Check if user has enabled biometric login in app preferences
     */
    suspend fun isBiometricLoginEnabled(): Boolean {
        return preferencesManager.isBiometricLoginEnabled()
    }

    /**
     * Enable or disable biometric login
     */
    suspend fun setBiometricLoginEnabled(enabled: Boolean) {
        preferencesManager.setBiometricLoginEnabled(enabled)
    }

    /**
     * Save credentials for biometric login (encrypted)
     */
    suspend fun saveBiometricCredentials(email: String, password: String) {
        preferencesManager.saveBiometricCredentials(email, password)
    }

    /**
     * Get saved credentials for biometric login
     */
    suspend fun getBiometricCredentials(): Pair<String, String>? {
        return preferencesManager.getBiometricCredentials()
    }

    /**
     * Clear saved biometric credentials
     */
    suspend fun clearBiometricCredentials() {
        preferencesManager.clearBiometricCredentials()
        preferencesManager.setBiometricLoginEnabled(false)
    }

    /**
     * Show biometric prompt and call callback on success/failure
     */
    fun showBiometricPrompt(
        activity: FragmentActivity,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        onFailed: () -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(context.getString(R.string.biometric_login_title))
            .setSubtitle(context.getString(R.string.biometric_login_subtitle))
            .setNegativeButtonText(context.getString(R.string.cancel))
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()

        val biometricPrompt = BiometricPrompt(activity, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    onError(errString.toString())
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    onFailed()
                }
            })

        biometricPrompt.authenticate(promptInfo)
    }
}
