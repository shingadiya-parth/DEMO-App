package com.example.services.ads

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Consent status for Google Mobile Ads (UMP / GDPR / EEA compliance).
 */
enum class AdConsentStatus {
    UNKNOWN,
    NOT_REQUIRED,
    REQUIRED,
    OBTAINED
}

/**
 * Manages Google Mobile Ads user consent state without blocking non-ad application usage.
 */
class AdConsentManager(private val context: Context) {

    private val _consentStatus = MutableStateFlow(AdConsentStatus.OBTAINED)
    val consentStatus: StateFlow<AdConsentStatus> = _consentStatus.asStateFlow()

    private val _canRequestAds = MutableStateFlow(true)
    val canRequestAds: StateFlow<Boolean> = _canRequestAds.asStateFlow()

    /**
     * Initializes consent information.
     */
    fun requestConsentInfoUpdate(onComplete: (Boolean) -> Unit = {}) {
        // Prepared for Google User Messaging Platform (UMP) SDK integration
        _consentStatus.value = AdConsentStatus.OBTAINED
        _canRequestAds.value = true
        onComplete(true)
    }

    /**
     * Resets consent for testing purposes.
     */
    fun resetConsentForTesting() {
        _consentStatus.value = AdConsentStatus.UNKNOWN
        _canRequestAds.value = false
    }

    fun grantConsent() {
        _consentStatus.value = AdConsentStatus.OBTAINED
        _canRequestAds.value = true
    }
}
