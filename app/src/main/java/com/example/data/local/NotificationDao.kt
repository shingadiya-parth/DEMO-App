package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.AppNotificationRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: AppNotificationRecord)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotifications(notifications: List<AppNotificationRecord>)

    @Query("SELECT * FROM app_notifications WHERE userId = :userId ORDER BY createdAt DESC")
    fun observeNotifications(userId: String): Flow<List<AppNotificationRecord>>

    @Query("SELECT * FROM app_notifications WHERE userId = :userId ORDER BY createdAt DESC LIMIT :limit OFFSET :offset")
    suspend fun getNotifications(userId: String, limit: Int = 100, offset: Int = 0): List<AppNotificationRecord>

    @Query("SELECT COUNT(*) FROM app_notifications WHERE userId = :userId AND isRead = 0")
    fun observeUnreadCount(userId: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM app_notifications WHERE userId = :userId AND isRead = 0")
    suspend fun getUnreadCount(userId: String): Int

    @Query("UPDATE app_notifications SET isRead = 1 WHERE notificationId = :notificationId AND userId = :userId")
    suspend fun markAsRead(notificationId: String, userId: String)

    @Query("UPDATE app_notifications SET isRead = 1 WHERE userId = :userId AND isRead = 0")
    suspend fun markAllAsRead(userId: String)

    @Query("DELETE FROM app_notifications WHERE notificationId = :notificationId AND userId = :userId")
    suspend fun deleteNotification(notificationId: String, userId: String)

    @Query("DELETE FROM app_notifications WHERE userId = :userId")
    suspend fun deleteNotificationsForUser(userId: String)

    @Query("DELETE FROM app_notifications WHERE expiresAt IS NOT NULL AND expiresAt < :currentTime")
    suspend fun deleteExpiredNotifications(currentTime: Long)
}
