package com.example.data.model

/**
 * Valid transaction types for the centralized coin ledger.
 */
enum class TransactionType(val displayName: String, val isCredit: Boolean) {
    GAME_REWARD("Game Reward", true),
    SPIN_REWARD("Spin Reward", true),
    SCRATCH_REWARD("Scratch Reward", true),
    PUZZLE_REWARD("Puzzle Reward", true),
    DAILY_BONUS("Daily Bonus", true),
    REFERRAL_REWARD("Referral Reward", true),
    AD_REWARD("Ad Reward", true),
    GIVEAWAY_REWARD("Giveaway Reward", true),
    REDEMPTION_DEDUCTION("Redemption Deduction", false),
    ADMIN_ADJUSTMENT("Admin Adjustment", true),
    REVERSAL("Transaction Reversal", true)
}
