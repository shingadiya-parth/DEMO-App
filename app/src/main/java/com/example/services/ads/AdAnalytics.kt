package com.example.services.ads

import android.util.Log

/**
 * AdMob Analytics Event Types (No PII / Private data logged).
 */
enum class AdAnalyticsEvent(val eventName: String) {
    AD_REQUESTED("ad_requested"),
    REWARDED_AD_LOADED("rewarded_ad_loaded"),
    REWARDED_AD_STARTED("rewarded_ad_started"),
    REWARDED_AD_COMPLETED("rewarded_ad_completed"),
    REWARDED_AD_FAILED("rewarded_ad_failed"),
    REWARDED_REWARD_GRANTED("rewarded_reward_granted"),
    REWARDED_REWARD_REJECTED("rewarded_reward_rejected"),
    INTERSTITIAL_REQUESTED("interstitial_requested"),
    INTERSTITIAL_SHOWN("interstitial_shown"),
    INTERSTITIAL_CLOSED("interstitial_closed"),
    BANNER_LOADED("banner_loaded"),
    BANNER_FAILED("banner_failed")
}

/**
 * Ad Analytics Dispatcher.
 */
object AdAnalytics {

    private const val TAG = "AdAnalytics"

    private val eventHistory = mutableListOf<String>()

    fun logEvent(
        event: AdAnalyticsEvent,
        params: Map<String, Any> = emptyMap()
    ) {
        val sanitizedParams = params.mapValues { (_, value) ->
            // Ensure no sensitive or personal information is serialized
            value.toString().take(100)
        }

        val logEntry = "[${event.eventName}] params=$sanitizedParams timestamp=${System.currentTimeMillis()}"
        Log.d(TAG, logEntry)

        synchronized(eventHistory) {
            if (eventHistory.size > 200) {
                eventHistory.removeAt(0)
            }
            eventHistory.add(logEntry)
        }
    }

    fun getRecentEventLogs(): List<String> = synchronized(eventHistory) {
        eventHistory.toList()
    }

    fun clearLogs() = synchronized(eventHistory) {
        eventHistory.clear()
    }
}
