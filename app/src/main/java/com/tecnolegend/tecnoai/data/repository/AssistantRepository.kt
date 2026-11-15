package com.tecnolegend.tecnoai.data.repository

import android.content.Context
import com.tecnolegend.tecnoai.data.local.ConversationDao
import com.tecnolegend.tecnoai.data.local.ConversationEntity
import com.tecnolegend.tecnoai.data.local.UserSettings
import com.tecnolegend.tecnoai.data.local.UserSettingsManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AssistantRepository(
    private val conversationDao: ConversationDao,
    private val userSettingsManager: UserSettingsManager,
    private val context: Context
) {
    val messagesFlow: Flow<List<ConversationEntity>> = conversationDao.getAllMessages()
    val settingsFlow: Flow<UserSettings> = userSettingsManager.userSettingsFlow

    private val auth: FirebaseAuth? by lazy {
        try {
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            null
        }
    }

    private val firestore: FirebaseFirestore? by lazy {
        try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun addMessage(
        role: String,
        content: String,
        toolName: String? = null,
        toolParams: String? = null,
        toolResult: String? = null,
        isToolSuccess: Boolean? = null,
        requiresConfirmation: Boolean = false,
        isConfirmed: Boolean = false,
        searchSources: String? = null
    ): Long = withContext(Dispatchers.IO) {
        val entity = ConversationEntity(
            role = role,
            content = content,
            timestamp = System.currentTimeMillis(),
            toolName = toolName,
            toolParams = toolParams,
            toolResult = toolResult,
            isToolSuccess = isToolSuccess,
            requiresConfirmation = requiresConfirmation,
            isConfirmed = isConfirmed,
            searchSources = searchSources
        )
        val id = conversationDao.insertMessage(entity)
        syncToCloudIfLoggedIn(entity.copy(id = id))
        id
    }

    suspend fun updateMessage(message: ConversationEntity) = withContext(Dispatchers.IO) {
        conversationDao.updateMessage(message)
        syncToCloudIfLoggedIn(message)
    }

    suspend fun getRecentMessages(limit: Int): List<ConversationEntity> = withContext(Dispatchers.IO) {
        conversationDao.getRecentMessages(limit)
    }

    suspend fun deleteMessage(id: Long) = withContext(Dispatchers.IO) {
        conversationDao.deleteMessageById(id)
    }

    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        conversationDao.clearAll()
    }

    suspend fun updateSettings(update: suspend (UserSettingsManager) -> Unit) {
        update(userSettingsManager)
    }

    private fun syncToCloudIfLoggedIn(entity: ConversationEntity) {
        try {
            val user = auth?.currentUser
            if (user != null && firestore != null) {
                val data = hashMapOf(
                    "id" to entity.id,
                    "role" to entity.role,
                    "content" to entity.content,
                    "timestamp" to entity.timestamp,
                    "toolName" to (entity.toolName ?: ""),
                    "toolParams" to (entity.toolParams ?: ""),
                    "toolResult" to (entity.toolResult ?: ""),
                    "isToolSuccess" to (entity.isToolSuccess ?: true),
                    "searchSources" to (entity.searchSources ?: "")
                )
                firestore?.collection("users")
                    ?.document(user.uid)
                    ?.collection("conversations")
                    ?.document(entity.id.toString())
                    ?.set(data, SetOptions.merge())
            }
        } catch (e: Exception) {
            // Ignore cloud sync failures for local-first reliability
        }
    }
}
