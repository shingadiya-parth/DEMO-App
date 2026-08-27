package com.example.core.security

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

enum class RateLimitAction(
    val maxRequests: Int,
    val windowMillis: Long
) {
    LOGIN(maxRequests = 5, windowMillis = 60_000L), // 5 attempts per minute
    SIGNUP(maxRequests = 3, windowMillis = 300_000L), // 3 attempts per 5 minutes
    REWARD_REQUEST(maxRequests = 20, windowMillis = 60_000L), // 20 reward requests per minute
    GAME_SESSION_CREATE(maxRequests = 15, windowMillis = 60_000L), // 15 games per minute
    GAME_COMPLETION(maxRequests = 15, windowMillis = 60_000L), // 15 submissions per minute
    DAILY_BONUS(maxRequests = 3, windowMillis = 60_000L), // 3 attempts per minute
    AD_REWARD(maxRequests = 10, windowMillis = 60_000L), // 10 ad completions per minute
    REFERRAL_APPLY(maxRequests = 5, windowMillis = 60_000L), // 5 code entries per minute
    REDEMPTION_REQUEST(maxRequests = 3, windowMillis = 60_000L) // 3 redemptions per minute
}

sealed class RateLimitResult {
    data object Allowed : RateLimitResult()
    data class Exceeded(
        val action: RateLimitAction,
        val retryAfterSeconds: Long,
        val message: String
    ) : RateLimitResult()
}

/**
 * High-performance sliding-window in-memory rate limiter.
 * Protects endpoints from rapid abuse and automation without degrading UX.
 */
object RateLimiter {
    // Key: "$userId:$action" -> Queue of timestamps
    private val requestWindows = ConcurrentHashMap<String, ConcurrentLinkedQueue<Long>>()

    /**
     * Checks if a request is allowed and records timestamp if allowed.
     */
    fun checkAndRecord(userId: String, action: RateLimitAction): RateLimitResult {
        val key = "$userId:${action.name}"
        val now = System.currentTimeMillis()
        val windowStart = now - action.windowMillis

        val timestamps = requestWindows.computeIfAbsent(key) { ConcurrentLinkedQueue() }

        // Evict expired timestamps outside window
        while (true) {
            val oldest = timestamps.peek() ?: break
            if (oldest < windowStart) {
                timestamps.poll()
            } else {
                break
            }
        }

        if (timestamps.size >= action.maxRequests) {
            val oldest = timestamps.peek() ?: now
            val remainingCooldown = ((oldest + action.windowMillis) - now).coerceAtLeast(1000L)
            val retryAfterSeconds = (remainingCooldown / 1000L).coerceAtLeast(1L)
            return RateLimitResult.Exceeded(
                action = action,
                retryAfterSeconds = retryAfterSeconds,
                message = "Too many requests. Please wait $retryAfterSeconds seconds before retrying."
            )
        }

        timestamps.add(now)
        return RateLimitResult.Allowed
    }

    /**
     * Resets rate limits for a user (e.g. on test teardown or admin override).
     */
    fun resetForUser(userId: String) {
        requestWindows.keys.filter { it.startsWith("$userId:") }.forEach {
            requestWindows.remove(it)
        }
    }

    fun clearAll() {
        requestWindows.clear()
    }
}
