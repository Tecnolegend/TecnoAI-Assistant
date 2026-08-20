package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ConversationEntity::class,
        UserRelationshipEntity::class,
        UserPreferenceEntity::class,
        UserActivityLogEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class TecnoDatabase : RoomDatabase() {
    abstract fun conversationDao(): ConversationDao
    abstract fun userRelationshipDao(): UserRelationshipDao
    abstract fun userPreferenceDao(): UserPreferenceDao
    abstract fun userActivityLogDao(): UserActivityLogDao

    companion object {
        @Volatile
        private var INSTANCE: TecnoDatabase? = null

        fun getDatabase(context: Context): TecnoDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TecnoDatabase::class.java,
                    "tecno_ai_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
