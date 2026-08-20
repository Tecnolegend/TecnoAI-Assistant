package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface UserRelationshipDao {
    @Query("SELECT * FROM user_relationships ORDER BY relationKey ASC")
    fun getAllRelationships(): Flow<List<UserRelationshipEntity>>

    @Query("SELECT * FROM user_relationships WHERE LOWER(relationKey) = LOWER(:key) OR LOWER(nickname) = LOWER(:key) LIMIT 1")
    suspend fun getRelationshipByKey(key: String): UserRelationshipEntity?

    @Query("SELECT * FROM user_relationships WHERE LOWER(displayName) LIKE '%' || LOWER(:name) || '%' LIMIT 1")
    suspend fun getRelationshipByName(name: String): UserRelationshipEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRelationship(relationship: UserRelationshipEntity): Long

    @Update
    suspend fun updateRelationship(relationship: UserRelationshipEntity)

    @Query("DELETE FROM user_relationships WHERE id = :id")
    suspend fun deleteRelationshipById(id: Int)

    @Query("DELETE FROM user_relationships WHERE LOWER(relationKey) = LOWER(:key)")
    suspend fun deleteRelationshipByKey(key: String)
}

@Dao
interface UserPreferenceDao {
    @Query("SELECT * FROM user_preferences ORDER BY category ASC")
    fun getAllPreferences(): Flow<List<UserPreferenceEntity>>

    @Query("SELECT * FROM user_preferences WHERE prefKey = :key LIMIT 1")
    suspend fun getPreference(key: String): UserPreferenceEntity?

    @Query("SELECT * FROM user_preferences WHERE category = :category")
    fun getPreferencesByCategory(category: String): Flow<List<UserPreferenceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreference(preference: UserPreferenceEntity)

    @Query("DELETE FROM user_preferences WHERE prefKey = :key")
    suspend fun deletePreference(key: String)
}

@Dao
interface UserActivityLogDao {
    @Query("SELECT * FROM user_activity_logs ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentActivities(limit: Int = 50): List<UserActivityLogEntity>

    @Query("SELECT target, COUNT(*) as count FROM user_activity_logs GROUP BY target ORDER BY count DESC LIMIT :limit")
    suspend fun getTopTargetActivities(limit: Int = 10): List<TargetCount>

    @Query("SELECT target, COUNT(*) as count FROM user_activity_logs WHERE hourOfDay BETWEEN :startHour AND :endHour GROUP BY target ORDER BY count DESC LIMIT :limit")
    suspend fun getFrequentTargetsForTimeWindow(startHour: Int, endHour: Int, limit: Int = 5): List<TargetCount>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivity(activity: UserActivityLogEntity)

    @Query("DELETE FROM user_activity_logs WHERE timestamp < :olderThanTimestamp")
    suspend fun purgeOldActivities(olderThanTimestamp: Long)
}

data class TargetCount(
    val target: String,
    val count: Int
)
