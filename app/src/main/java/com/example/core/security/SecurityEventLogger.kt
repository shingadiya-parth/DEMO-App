package com.example.core.security

import android.util.Log
import com.example.data.local.SecurityEventDao
import com.example.data.model.SecurityEvent
import com.example.data.model.SecurityEventType
import com.example.data.model.SecuritySeverity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Security Event Logger.
 * Records security-critical anomalies, rejections, duplicate attempts, and potential fraud.
 * Strictly sanitizes metadata to avoid logging sensitive user credentials, tokens, or secrets.
 */
class SecurityEventLogger(
    private val securityEventDao: SecurityEventDao? = null,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    companion object {
        private const val TAG = "PlayRewardsSecurity"

        fun sanitize(input: String?): String? {
            if (input == null) return null
            // Mask password patterns, auth tokens, and long hex strings
            return input
                .replace(Regex("(?i)(password|pwd|pin|secret|token|key)=[^&\\s]+"), "$1=***REDACTED***")
                .replace(Regex("(?i)bearer\\s+[a-zA-Z0-9._\\-]+"), "Bearer ***REDACTED***")
        }
    }

    fun logEvent(
        userId: String,
        eventType: SecurityEventType,
        severity: SecuritySeverity,
        relatedId: String? = null,
        metadata: String? = null
    ) {
        val sanitizedMetadata = sanitize(metadata)
        val event = SecurityEvent(
            eventId = "sec_${UUID.randomUUID()}",
            userId = userId,
            eventType = eventType,
            severity = severity,
            timestamp = System.currentTimeMillis(),
            relatedId = relatedId,
            metadata = sanitizedMetadata
        )

        // Log to Android Logcat
        val logMsg = "SECURITY_EVENT [${eventType.name}] [Severity: ${severity.name}] User: $userId | Rel: $relatedId | Meta: $sanitizedMetadata"
        when (severity) {
            SecuritySeverity.LOW -> Log.d(TAG, logMsg)
            SecuritySeverity.MEDIUM -> Log.w(TAG, logMsg)
            SecuritySeverity.HIGH, SecuritySeverity.CRITICAL -> Log.e(TAG, logMsg)
        }

        // Persist to local security ledger asynchronously
        if (securityEventDao != null) {
            scope.launch {
                try {
                    securityEventDao.insertEvent(event)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to persist security event", e)
                }
            }
        }
    }

    suspend fun logEventSync(
        userId: String,
        eventType: SecurityEventType,
        severity: SecuritySeverity,
        relatedId: String? = null,
        metadata: String? = null
    ) {
        val sanitizedMetadata = sanitize(metadata)
        val event = SecurityEvent(
            eventId = "sec_${UUID.randomUUID()}",
            userId = userId,
            eventType = eventType,
            severity = severity,
            timestamp = System.currentTimeMillis(),
            relatedId = relatedId,
            metadata = sanitizedMetadata
        )
        securityEventDao?.insertEvent(event)
    }
}
