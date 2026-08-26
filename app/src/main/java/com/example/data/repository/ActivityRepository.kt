package com.example.data.repository

import com.example.data.local.ActivityDao
import com.example.data.model.ActivityCategory
import com.example.data.model.ActivityType
import com.example.data.model.UserActivityRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Unified Activity History repository.
 * Records, queries, and filters user milestones, game events, rewards, referrals, and redemptions.
 * Separate from the immutable financial CoinTransaction ledger.
 */
class ActivityRepository(
    private val activityDao: ActivityDao
) {

    fun observeActivities(userId: String): Flow<List<UserActivityRecord>> {
        return activityDao.observeActivities(userId)
    }

    fun observeActivitiesByCategory(userId: String, category: ActivityCategory): Flow<List<UserActivityRecord>> {
        return if (category == ActivityCategory.ALL) {
            activityDao.observeActivities(userId)
        } else {
            activityDao.observeActivitiesByCategory(userId, category)
        }
    }

    suspend fun getActivities(userId: String, limit: Int = 100, offset: Int = 0): List<UserActivityRecord> = withContext(Dispatchers.IO) {
        if (userId.isBlank()) emptyList()
        else activityDao.getActivities(userId, limit, offset)
    }

    suspend fun recordActivity(
        userId: String,
        activityType: ActivityType,
        category: ActivityCategory,
        title: String,
        description: String,
        relatedId: String? = null,
        result: String? = null,
        createdAt: Long = System.currentTimeMillis()
    ): UserActivityRecord = withContext(Dispatchers.IO) {
        if (userId.isBlank()) throw IllegalArgumentException("User ID cannot be blank")

        // Idempotency: Check if an activity for this related session / transaction already exists
        if (!relatedId.isNullOrBlank()) {
            val existing = activityDao.getActivityByRelatedId(userId, relatedId, activityType)
            if (existing != null) {
                return@withContext existing
            }
        }

        val record = UserActivityRecord(
            activityId = "act_${UUID.randomUUID().toString().take(12)}",
            userId = userId,
            activityType = activityType,
            category = category,
            title = title,
            description = description,
            relatedId = relatedId,
            result = result,
            createdAt = createdAt
        )

        activityDao.insertActivity(record)
        record
    }

    suspend fun deleteActivity(activityId: String, userId: String) = withContext(Dispatchers.IO) {
        activityDao.deleteActivity(activityId, userId)
    }

    suspend fun clearActivitiesForUser(userId: String) = withContext(Dispatchers.IO) {
        activityDao.deleteActivitiesForUser(userId)
    }
}
