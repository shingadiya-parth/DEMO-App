package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.SecurityEvent
import com.example.data.model.SecurityEventType
import com.example.data.model.SecuritySeverity
import kotlinx.coroutines.flow.Flow

@Dao
interface SecurityEventDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: SecurityEvent)

    @Query("SELECT * FROM security_events WHERE userId = :userId ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getEventsForUser(userId: String, limit: Int = 50): List<SecurityEvent>

    @Query("SELECT * FROM security_events WHERE userId = :userId ORDER BY timestamp DESC")
    fun observeEventsForUser(userId: String): Flow<List<SecurityEvent>>

    @Query("SELECT * FROM security_events WHERE eventType = :type ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getEventsByType(type: SecurityEventType, limit: Int = 50): List<SecurityEvent>

    @Query("SELECT * FROM security_events WHERE severity IN (:severities) ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getEventsBySeverity(severities: List<SecuritySeverity>, limit: Int = 50): List<SecurityEvent>

    @Query("SELECT COUNT(*) FROM security_events WHERE userId = :userId AND timestamp >= :sinceTimestamp")
    suspend fun getRecentEventCount(userId: String, sinceTimestamp: Long): Int

    @Query("SELECT COUNT(*) FROM security_events WHERE userId = :userId AND eventType = :type AND timestamp >= :sinceTimestamp")
    suspend fun getRecentEventCountByType(userId: String, type: SecurityEventType, sinceTimestamp: Long): Int

    @Query("SELECT * FROM security_events WHERE userId = :userId AND timestamp >= :sinceTimestamp")
    suspend fun getRecentEventsForUser(userId: String, sinceTimestamp: Long): List<SecurityEvent>
}
