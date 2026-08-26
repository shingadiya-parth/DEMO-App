package com.example.core.config

/**
 * Configuration for individual wheel segments in the Spin & Win game.
 */
data class SpinRewardSegment(
    val id: Int,
    val amount: Long,
    val weight: Int,
    val label: String,
    val colorHex: Long,
    val enabled: Boolean = true
)

/**
 * Centralized Spin & Win Game Configuration.
 * Governs limits, probabilities, reward segments, and ad extensions.
 */
object SpinGameConfig {
    const val GAME_ID = "spin_win"
    const val GAME_TITLE = "Spin & Win"

    var spinGameEnabled: Boolean = true
    var dailySpinLimit: Int = 5
    var cooldownSeconds: Long = 0L
    var adExtraSpinEnabled: Boolean = false
    var maximumAdExtraSpinsPerDay: Int = 3
    var minimumAppVersion: Int = 1

    /**
     * Authoritative reward segments with weights and UI styling.
     */
    val rewardSegments: List<SpinRewardSegment> = listOf(
        SpinRewardSegment(id = 0, amount = 10L, weight = 25, label = "10", colorHex = 0xFF4F46E5), // Indigo
        SpinRewardSegment(id = 1, amount = 20L, weight = 20, label = "20", colorHex = 0xFF10B981), // Emerald
        SpinRewardSegment(id = 2, amount = 25L, weight = 18, label = "25", colorHex = 0xFFF59E0B), // Amber Gold
        SpinRewardSegment(id = 3, amount = 50L, weight = 15, label = "50", colorHex = 0xFF8B5CF6), // Purple
        SpinRewardSegment(id = 4, amount = 75L, weight = 10, label = "75", colorHex = 0xFFEC4899), // Pink
        SpinRewardSegment(id = 5, amount = 100L, weight = 7, label = "100", colorHex = 0xFF06B6D4), // Cyan
        SpinRewardSegment(id = 6, amount = 150L, weight = 4, label = "150", colorHex = 0xFF3B82F6), // Blue
        SpinRewardSegment(id = 7, amount = 200L, weight = 1, label = "200", colorHex = 0xFFEF4444)  // Coral Red
    )

    fun getActiveSegments(): List<SpinRewardSegment> = rewardSegments.filter { it.enabled }
}
