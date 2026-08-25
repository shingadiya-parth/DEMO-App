package com.example.core.config

/**
 * Centralized Coin Conversion Configuration.
 * 
 * Default Rate: 700 coins = ₹1.00 (INR)
 * 7,000 coins = ₹10.00
 * 20,000 coins ≈ ₹28.57
 * 
 * NEVER hardcode coin conversion rates across individual screens or games.
 * Use this central configuration everywhere.
 */
object CoinConfig {
    /**
     * Number of coins equivalent to 1 Unit of base currency (₹1.00 INR).
     */
    var coinsPerCurrencyUnit: Double = 700.0
        private set

    /**
     * Currency symbol used across the application.
     */
    const val CURRENCY_SYMBOL: String = "₹"
    const val CURRENCY_CODE: String = "INR"

    /**
     * Minimum coin threshold to request a redemption.
     */
    const val MINIMUM_REDEMPTION_COINS: Long = 7000L // ₹10.00

    /**
     * Daily coin earning cap across all games (Anti-abuse protection).
     */
    const val DAILY_MAX_EARN_CAP_COINS: Long = 50000L

    /**
     * Updates the conversion rate dynamically (e.g. from remote config in future).
     */
    fun updateConversionRate(newCoinsPerUnit: Double) {
        require(newCoinsPerUnit > 0) { "Conversion rate must be greater than 0" }
        coinsPerCurrencyUnit = newCoinsPerUnit
    }

    /**
     * Converts coins into currency value (INR).
     */
    fun coinsToCurrencyValue(coins: Long): Double {
        if (coins <= 0) return 0.0
        return coins / coinsPerCurrencyUnit
    }

    /**
     * Formats coins into formatted currency string: e.g. "₹10.00" or "₹28.57".
     */
    fun formatCurrencyFromCoins(coins: Long): String {
        val value = coinsToCurrencyValue(coins)
        return String.format("%s%.2f", CURRENCY_SYMBOL, value)
    }

    /**
     * Converts currency amount (INR) to required coins.
     */
    fun currencyToRequiredCoins(currencyAmount: Double): Long {
        if (currencyAmount <= 0) return 0L
        return (currencyAmount * coinsPerCurrencyUnit).toLong()
    }
}
