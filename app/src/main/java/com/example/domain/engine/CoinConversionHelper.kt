package com.example.domain.engine

import com.example.core.config.CoinConfig

/**
 * Domain helper for coin conversion, currency formatting, and redemption progress calculations.
 */
object CoinConversionHelper {

    fun formatCoins(coins: Long): String {
        return "%,d".format(coins)
    }

    fun getCurrencyEstimate(coins: Long): String {
        return CoinConfig.formatCurrencyFromCoins(coins)
    }

    fun calculateProgressTowards(currentCoins: Long, targetCoins: Long): Float {
        if (targetCoins <= 0L) return 0f
        return (currentCoins.toFloat() / targetCoins.toFloat()).coerceIn(0f, 1f)
    }

    fun getRateExplanation(): String {
        val baseCoins = CoinConfig.coinsPerCurrencyUnit.toInt()
        return "$baseCoins Coins = ${CoinConfig.CURRENCY_SYMBOL}1.00 ${CoinConfig.CURRENCY_CODE}"
    }
}
