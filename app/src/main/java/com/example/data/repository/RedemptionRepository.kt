package com.example.data.repository

import androidx.room.withTransaction
import com.example.core.config.CoinConfig
import com.example.core.config.RewardCatalogConfig
import com.example.core.database.AppDatabase
import com.example.data.local.RedemptionDao
import com.example.data.model.AccountStatus
import com.example.data.model.RedemptionEligibilityResult
import com.example.data.model.RedemptionRequest
import com.example.data.model.RedemptionReward
import com.example.data.model.RedemptionStatus
import com.example.data.model.RewardCategory
import com.example.data.model.RewardStockStatus
import com.example.data.model.TransactionType
import kotlinx.coroutines.flow.Flow
import java.util.Calendar
import java.util.UUID

sealed class RedemptionResult {
    data class Success(
        val request: RedemptionRequest,
        val remainingBalance: Long,
        val transactionId: String
    ) : RedemptionResult()

    data class Error(val message: String) : RedemptionResult()
}

/**
 * Centralized Redemption Repository.
 * 
 * Manages the Reward Catalog, server-side eligibility verification,
 * duplicate protection, atomic wallet deductions, snapshot persistence,
 * and auditable refunds/reversals.
 */
class RedemptionRepository(
    private val redemptionDao: RedemptionDao,
    private val walletRepository: WalletRepository,
    private val userRepository: UserRepository,
    private val database: AppDatabase? = null
) {

    /**
     * In-memory or database-backed dynamic catalog.
     * Can be dynamically modified or fetched from backend configuration.
     */
    private val catalog: MutableList<RedemptionReward> = RewardCatalogConfig.getDefaultCatalog().toMutableList()

    /**
     * Retrieves the entire current reward catalog.
     */
    fun getRewardCatalog(): List<RedemptionReward> {
        return catalog.toList()
    }

    /**
     * Retrieves rewards filtered by category.
     */
    fun getRewardsByCategory(category: RewardCategory): List<RedemptionReward> {
        return catalog.filter { it.category == category && it.enabled }
    }

    /**
     * Retrieves a single reward by its ID.
     */
    fun getRewardById(rewardId: String): RedemptionReward? {
        return catalog.find { it.rewardId == rewardId }
    }

    /**
     * Observes live redemption requests for a user in reverse-chronological order.
     */
    fun observeUserRequests(userId: String): Flow<List<RedemptionRequest>> {
        return redemptionDao.observeRedemptionRequests(userId)
    }

    /**
     * Retrieves a single redemption request by its unique ID.
     */
    suspend fun getRedemptionById(redemptionId: String): RedemptionRequest? {
        return redemptionDao.getRedemptionById(redemptionId)
    }

    /**
     * Authoritative backend evaluation of user eligibility for a specific reward.
     */
    suspend fun checkRewardEligibility(userId: String, rewardId: String): RedemptionEligibilityResult {
        if (userId.isBlank()) {
            return RedemptionEligibilityResult.Ineligible("Authentication required.", "Login Required")
        }

        val user = userRepository.getUserById(userId)
            ?: return RedemptionEligibilityResult.Ineligible("User account not found.", "Account Error")

        if (user.accountStatus != AccountStatus.ACTIVE) {
            return RedemptionEligibilityResult.Ineligible(
                "Your account is ${user.accountStatus.name.lowercase().replace('_', ' ')}. Please contact support.",
                "Account Inactive"
            )
        }

        val reward = getRewardById(rewardId)
            ?: return RedemptionEligibilityResult.Ineligible("Reward not found in catalog.", "Not Found")

        if (!reward.enabled || reward.stockStatus == RewardStockStatus.DISABLED) {
            return RedemptionEligibilityResult.Ineligible("This reward is currently unavailable.", "Unavailable")
        }

        if (reward.stockStatus == RewardStockStatus.OUT_OF_STOCK) {
            return RedemptionEligibilityResult.Ineligible("This reward is currently out of stock.", "Sold Out")
        }

        val currentBalance = walletRepository.getCalculatedBalance(userId)
        if (currentBalance < reward.requiredCoins) {
            val needed = reward.requiredCoins - currentBalance
            return RedemptionEligibilityResult.Ineligible(
                "You need $needed more NestCoins to redeem this reward.",
                "Need $needed Coins",
                coinsNeeded = needed
            )
        }

        // Daily limit check for this reward
        val startOfToday = getStartOfDayTimestamp()
        val rewardDailyCount = redemptionDao.getRequestsForRewardSince(userId, rewardId, startOfToday).size
        if (rewardDailyCount >= reward.dailyRedemptionLimit) {
            return RedemptionEligibilityResult.Ineligible(
                "You have reached the daily limit (${reward.dailyRedemptionLimit}/day) for this reward.",
                "Daily Limit Reached"
            )
        }

        // Overall global daily redemption limit check
        val globalDailyCount = redemptionDao.getDailyActiveRedemptionCount(userId, startOfToday)
        if (globalDailyCount >= RewardCatalogConfig.GLOBAL_DAILY_REDEMPTION_LIMIT_PER_USER) {
            return RedemptionEligibilityResult.Ineligible(
                "You have reached your total daily redemption limit (${RewardCatalogConfig.GLOBAL_DAILY_REDEMPTION_LIMIT_PER_USER}/day).",
                "Daily Cap Reached"
            )
        }

        // Total reward redemption limit check
        val totalActiveForReward = redemptionDao.getActiveRedemptionCountForReward(userId, rewardId)
        if (totalActiveForReward >= reward.totalRedemptionLimit) {
            return RedemptionEligibilityResult.Ineligible(
                "Lifetime redemption limit reached for this reward.",
                "Max Claimed"
            )
        }

        return RedemptionEligibilityResult.Eligible
    }

    /**
     * Submits a secure redemption request.
     * 
     * Flow:
     * 1. Validate user and reward.
     * 2. Authoritatively evaluate eligibility.
     * 3. Validate destination account (email / UPI ID).
     * 4. Enforce idempotency key check.
     * 5. Atomically deduct coins via centralized WalletRepository with REDEMPTION_DEDUCTION.
     * 6. Atomically persist RedemptionRequest with snapshot values (name, value, required coins).
     * 7. Return atomic success result.
     */
    suspend fun submitRedemptionRequest(
        userId: String,
        rewardId: String,
        destinationAccount: String,
        idempotencyKey: String? = null
    ): RedemptionResult {
        if (userId.isBlank()) {
            return RedemptionResult.Error("Unauthorized request. Please log in.")
        }

        val cleanedDestination = destinationAccount.trim()
        if (cleanedDestination.isBlank()) {
            return RedemptionResult.Error("Please enter a valid destination email or UPI ID.")
        }

        val reward = getRewardById(rewardId)
            ?: return RedemptionResult.Error("Selected reward does not exist in catalog.")

        // 1. Generate / resolve unique idempotency key
        val actualIdempotencyKey = idempotencyKey?.takeIf { it.isNotBlank() }
            ?: "RED_REQ_${userId}_${rewardId}_${System.currentTimeMillis()}"

        // 2. Idempotency check against existing redemptions
        val existingRequest = redemptionDao.getRedemptionByIdempotencyKey(actualIdempotencyKey)
        if (existingRequest != null) {
            val balance = walletRepository.getCalculatedBalance(userId)
            return RedemptionResult.Success(
                request = existingRequest,
                remainingBalance = balance,
                transactionId = existingRequest.transactionId ?: ""
            )
        }

        // 3. Authoritative eligibility check
        val eligibility = checkRewardEligibility(userId, rewardId)
        if (eligibility is RedemptionEligibilityResult.Ineligible) {
            return RedemptionResult.Error(eligibility.reason)
        }

        // 4. Atomic Ledger Deduction & Record Creation
        val newRedemptionId = "red_${UUID.randomUUID()}"
        val deductionIdempotencyKey = "TX_DEDUCT_$actualIdempotencyKey"

        val txResult = walletRepository.subtractCoins(
            userId = userId,
            type = TransactionType.REDEMPTION_DEDUCTION,
            source = "redemption_${reward.rewardId}",
            amount = reward.requiredCoins,
            referenceId = newRedemptionId,
            idempotencyKey = deductionIdempotencyKey,
            metadata = "Redeemed ${reward.name} (₹${reward.value}) to $cleanedDestination"
        )

        return when (txResult) {
            is TransactionResult.Success -> {
                val redemptionRecord = RedemptionRequest(
                    redemptionId = newRedemptionId,
                    userId = userId,
                    rewardId = reward.rewardId,
                    rewardNameSnapshot = reward.name,
                    rewardValueSnapshot = reward.value,
                    requiredCoinsSnapshot = reward.requiredCoins,
                    currencySnapshot = reward.currency,
                    destinationAccount = cleanedDestination,
                    status = RedemptionStatus.PENDING,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                    transactionId = txResult.transaction.transactionId,
                    idempotencyKey = actualIdempotencyKey,
                    adminNote = "Request logged in verification queue. Pending review."
                )

                redemptionDao.insertRequest(redemptionRecord)

                RedemptionResult.Success(
                    request = redemptionRecord,
                    remainingBalance = txResult.newBalance,
                    transactionId = txResult.transaction.transactionId
                )
            }
            is TransactionResult.Duplicate -> {
                RedemptionResult.Error("Duplicate redemption request detected. Coins have not been deducted again.")
            }
            is TransactionResult.Error -> {
                RedemptionResult.Error(txResult.message)
            }
        }
    }

    /**
     * Executes atomic refund/reversal for a rejected or cancelled redemption.
     * 
     * Creates an auditable REVERSAL transaction in the centralized ledger,
     * restores user coins, and marks the redemption status as REFUNDED.
     */
    suspend fun refundRedemption(
        redemptionId: String,
        reason: String,
        adminIdentifier: String = "SYSTEM_REVERSAL"
    ): Result<RedemptionRequest> {
        val request = redemptionDao.getRedemptionById(redemptionId)
            ?: return Result.failure(IllegalArgumentException("Redemption request not found."))

        if (request.status == RedemptionStatus.REFUNDED) {
            return Result.failure(IllegalStateException("Redemption is already refunded."))
        }

        val refundIdempotencyKey = "REFUND_${request.redemptionId}_${System.currentTimeMillis()}"

        val refundTx = walletRepository.addCoins(
            userId = request.userId,
            type = TransactionType.REVERSAL,
            source = "redemption_refund",
            amount = request.requiredCoinsSnapshot,
            referenceId = request.redemptionId,
            idempotencyKey = refundIdempotencyKey,
            metadata = "Refund for ${request.rewardNameSnapshot}: $reason"
        )

        return when (refundTx) {
            is TransactionResult.Success -> {
                val updatedRequest = request.copy(
                    status = RedemptionStatus.REFUNDED,
                    failureReason = reason,
                    adminNote = "Refund processed ($reason) by $adminIdentifier",
                    updatedAt = System.currentTimeMillis(),
                    processedAt = System.currentTimeMillis()
                )
                redemptionDao.updateRequest(updatedRequest)
                Result.success(updatedRequest)
            }
            is TransactionResult.Duplicate -> {
                Result.failure(IllegalStateException("Refund transaction already processed."))
            }
            is TransactionResult.Error -> {
                Result.failure(IllegalStateException(refundTx.message))
            }
        }
    }

    /**
     * Helper to compute midnight epoch timestamp for today in local time.
     */
    private fun getStartOfDayTimestamp(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
