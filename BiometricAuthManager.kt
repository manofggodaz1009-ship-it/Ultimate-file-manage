package com.example.util

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

object BiometricAuthManager {

  enum class BiometricStatus {
    AVAILABLE,
    NOT_ENROLLED,
    NO_HARDWARE,
    HARDWARE_UNAVAILABLE,
    UNKNOWN
  }

  fun checkBiometricAvailability(context: Context): BiometricStatus {
    val biometricManager = BiometricManager.from(context)
    return when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
      BiometricManager.BIOMETRIC_SUCCESS -> BiometricStatus.AVAILABLE
      BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricStatus.NOT_ENROLLED
      BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricStatus.NO_HARDWARE
      BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricStatus.HARDWARE_UNAVAILABLE
      else -> BiometricStatus.UNKNOWN
    }
  }

  /**
   * Prompts user for 100% Biometric (Fingerprint) authentication without PIN/Password fallback.
   */
  fun authenticateWithFingerprint(
    activity: FragmentActivity,
    title: String = "கைரேகை சரிபார்ப்பு",
    subtitle: String = "பாதுகாப்பான நோட்பேடைத் திறக்க உங்கள் கைரேகையை வைக்கவும்",
    negativeButtonText: String = "ரத்து செய்",
    onSuccess: () -> Unit,
    onError: (errorCode: Int, errString: CharSequence) -> Unit,
    onFailed: () -> Unit = {}
  ) {
    val executor = ContextCompat.getMainExecutor(activity)
    val callback = object : BiometricPrompt.AuthenticationCallback() {
      override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
        super.onAuthenticationSucceeded(result)
        onSuccess()
      }

      override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
        super.onAuthenticationError(errorCode, errString)
        onError(errorCode, errString)
      }

      override fun onAuthenticationFailed() {
        super.onAuthenticationFailed()
        onFailed()
      }
    }

    val biometricPrompt = BiometricPrompt(activity, executor, callback)
    val promptInfo = BiometricPrompt.PromptInfo.Builder()
      .setTitle(title)
      .setSubtitle(subtitle)
      .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
      .setNegativeButtonText(negativeButtonText)
      .build()

    try {
      biometricPrompt.authenticate(promptInfo)
    } catch (e: Exception) {
      onError(-1, e.localizedMessage ?: "கைரேகை சேவை துவங்குவதில் பிழை")
    }
  }
}
