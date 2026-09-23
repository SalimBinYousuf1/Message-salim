package com.example.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "conversation_metadata")
data class ConversationMetadata(
    @PrimaryKey val threadId: Long,
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val draft: String? = null,
    val customName: String? = null
)

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversation_metadata")
    fun getAllMetadata(): Flow<List<ConversationMetadata>>

    @Query("SELECT * FROM conversation_metadata WHERE threadId = :threadId")
    suspend fun getMetadata(threadId: Long): ConversationMetadata?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(metadata: ConversationMetadata)

    @Query("UPDATE conversation_metadata SET isPinned = :pinned WHERE threadId = :threadId")
    suspend fun setPinned(threadId: Long, pinned: Boolean)

    @Query("UPDATE conversation_metadata SET isArchived = :archived WHERE threadId = :threadId")
    suspend fun setArchived(threadId: Long, archived: Boolean)

    @Query("UPDATE conversation_metadata SET draft = :draft WHERE threadId = :threadId")
    suspend fun saveDraft(threadId: Long, draft: String?)

    @Query("DELETE FROM conversation_metadata WHERE threadId = :threadId")
    suspend fun deleteMetadata(threadId: Long)
}

@Database(entities = [ConversationMetadata::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun conversationDao(): ConversationDao
}
