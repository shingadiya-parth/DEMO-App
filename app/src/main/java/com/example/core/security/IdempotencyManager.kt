package com.example.core.security

import java.security.MessageDigest
import java.util.UUID

/**
 * Generates and tracks idempotency keys to prevent duplicate rewards and transaction replay.
 */
object IdempotencyManager {

    /**
     * Generates a unique transaction token for a specific game/activity action.
     * When sessionId is provided, returns a deterministic idempotency key.
     */
    fun generateToken(userId: String, source: String, sessionId: String? = null): String {
        val raw = if (sessionId != null) {
            "$userId:$source:$sessionId"
        } else {
            "$userId:$source:${UUID.randomUUID()}:${System.currentTimeMillis()}"
        }
        return sha256(raw)
    }

    /**
     * Generates a deterministic daily check-in token for today to ensure it can only be granted once per day.
     */
    fun generateDailyToken(userId: String, actionType: String, dateString: String): String {
        return "DAILY:$userId:$actionType:$dateString"
    }

    private fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
