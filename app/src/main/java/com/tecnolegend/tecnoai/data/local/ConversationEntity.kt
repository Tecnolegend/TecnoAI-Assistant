package com.tecnolegend.tecnoai.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@Entity(tableName = "conversations")
@JsonClass(generateAdapter = true)
data class ConversationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val role: String, // "USER", "ASSISTANT", "SYSTEM", "TOOL"
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val toolName: String? = null,
    val toolParams: String? = null,
    val toolResult: String? = null,
    val isToolSuccess: Boolean? = null,
    val requiresConfirmation: Boolean = false,
    val isConfirmed: Boolean = false,
    val isCanceled: Boolean = false,
    val searchSources: String? = null // Comma-separated or JSON list of URLs/titles
)
