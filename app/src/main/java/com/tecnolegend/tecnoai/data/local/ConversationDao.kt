package com.tecnolegend.tecnoai.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversations ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentMessages(limit: Int): List<ConversationEntity>

    @Query("SELECT * FROM conversations WHERE id = :id")
    suspend fun getMessageById(id: Long): ConversationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ConversationEntity): Long

    @Update
    suspend fun updateMessage(message: ConversationEntity)

    @Query("DELETE FROM conversations WHERE id = :id")
    suspend fun deleteMessageById(id: Long)

    @Query("DELETE FROM conversations")
    suspend fun clearAll()
}
