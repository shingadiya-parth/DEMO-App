package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.ActivityCategory
import com.example.data.model.ActivityType
import com.example.data.model.UserActivityRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivity(activity: UserActivityRecord)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivities(activities: List<UserActivityRecord>)

    @Query("SELECT * FROM user_activities WHERE userId = :userId ORDER BY createdAt DESC")
    fun observeActivities(userId: String): Flow<List<UserActivityRecord>>

    @Query("SELECT * FROM user_activities WHERE userId = :userId AND category = :category ORDER BY createdAt DESC")
    fun observeActivitiesByCategory(userId: String, category: ActivityCategory): Flow<List<UserActivityRecord>>

    @Query("SELECT * FROM user_activities WHERE userId = :userId ORDER BY createdAt DESC LIMIT :limit OFFSET :offset")
    suspend fun getActivities(userId: String, limit: Int = 100, offset: Int = 0): List<UserActivityRecord>

    @Query("SELECT * FROM user_activities WHERE userId = :userId AND relatedId = :relatedId AND activityType = :activityType LIMIT 1")
    suspend fun getActivityByRelatedId(userId: String, relatedId: String, activityType: ActivityType): UserActivityRecord?

    @Query("DELETE FROM user_activities WHERE activityId = :activityId AND userId = :userId")
    suspend fun deleteActivity(activityId: String, userId: String)

    @Query("DELETE FROM user_activities WHERE userId = :userId")
    suspend fun deleteActivitiesForUser(userId: String)
}
