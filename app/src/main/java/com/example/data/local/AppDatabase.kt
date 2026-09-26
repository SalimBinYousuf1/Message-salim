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
    val customName: String? = null,
    val preferredSubId: Int? = null
)

@Entity(tableName = "scheduled_messages")
data class ScheduledMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val threadId: Long,
    val address: String,
    val body: String,
    val subId: Int = -1,
    val scheduledTimestamp: Long,
    val createdTimestamp: Long = System.currentTimeMillis(),
    val status: String = "PENDING" // PENDING, SENT, CANCELLED, FAILED
)

@Entity(tableName = "blocked_contacts")
data class BlockedContact(
    @PrimaryKey val address: String,
    val displayName: String,
    val blockedTimestamp: Long = System.currentTimeMillis()
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

    @Query("UPDATE conversation_metadata SET preferredSubId = :subId WHERE threadId = :threadId")
    suspend fun setPreferredSubId(threadId: Long, subId: Int?)

    @Query("DELETE FROM conversation_metadata WHERE threadId = :threadId")
    suspend fun deleteMetadata(threadId: Long)
}

@Dao
interface ScheduledMessageDao {
    @Query("SELECT * FROM scheduled_messages WHERE status = 'PENDING' ORDER BY scheduledTimestamp ASC")
    fun getAllPendingScheduledFlow(): Flow<List<ScheduledMessage>>

    @Query("SELECT * FROM scheduled_messages WHERE threadId = :threadId AND status = 'PENDING' ORDER BY scheduledTimestamp ASC")
    fun getPendingByThreadFlow(threadId: Long): Flow<List<ScheduledMessage>>

    @Query("SELECT * FROM scheduled_messages WHERE id = :id")
    suspend fun getById(id: Long): ScheduledMessage?

    @Query("SELECT * FROM scheduled_messages WHERE status = 'PENDING' AND scheduledTimestamp <= :currentTime")
    suspend fun getPendingDue(currentTime: Long): List<ScheduledMessage>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: ScheduledMessage): Long

    @Query("UPDATE scheduled_messages SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String)

    @Query("DELETE FROM scheduled_messages WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface BlockedContactDao {
    @Query("SELECT * FROM blocked_contacts ORDER BY blockedTimestamp DESC")
    fun getAllBlockedFlow(): Flow<List<BlockedContact>>

    @Query("SELECT EXISTS(SELECT 1 FROM blocked_contacts WHERE address = :address)")
    suspend fun isBlocked(address: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun block(contact: BlockedContact)

    @Query("DELETE FROM blocked_contacts WHERE address = :address")
    suspend fun unblock(address: String)
}

@Entity(tableName = "message_reactions")
data class MessageReaction(
    @PrimaryKey val messageId: Long,
    val emoji: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface MessageReactionDao {
    @Query("SELECT * FROM message_reactions")
    fun getAllReactionsFlow(): Flow<List<MessageReaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setReaction(reaction: MessageReaction)

    @Query("DELETE FROM message_reactions WHERE messageId = :messageId")
    suspend fun removeReaction(messageId: Long)
}

@Database(
    entities = [
        ConversationMetadata::class,
        ScheduledMessage::class,
        BlockedContact::class,
        MessageReaction::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun conversationDao(): ConversationDao
    abstract fun scheduledMessageDao(): ScheduledMessageDao
    abstract fun blockedContactDao(): BlockedContactDao
    abstract fun messageReactionDao(): MessageReactionDao
}
