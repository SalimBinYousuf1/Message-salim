package com.example.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Entity(
    tableName = "conversation_metadata",
    indices = [
        Index(value = ["threadId"]),
        Index(value = ["isPinned"]),
        Index(value = ["isArchived"])
    ]
)
data class ConversationMetadata(
    @PrimaryKey val threadId: Long,
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val draft: String? = null,
    val customName: String? = null,
    val preferredSubId: Int? = null
)

@Entity(
    tableName = "scheduled_messages",
    indices = [
        Index(value = ["threadId"]),
        Index(value = ["status", "scheduledTimestamp"])
    ]
)
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

@Entity(
    tableName = "blocked_contacts",
    indices = [
        Index(value = ["address"], unique = true)
    ]
)
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

    @Query("UPDATE scheduled_messages SET scheduledTimestamp = :newTimestamp WHERE id = :id")
    suspend fun updateScheduledTime(id: Long, newTimestamp: Long)

    @Query("UPDATE scheduled_messages SET body = :body, scheduledTimestamp = :newTimestamp WHERE id = :id")
    suspend fun updateBodyAndTime(id: Long, body: String, newTimestamp: Long)

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

@Entity(
    tableName = "local_media_messages",
    indices = [
        Index(value = ["threadId"]),
        Index(value = ["timestamp"])
    ]
)
data class LocalMediaMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val threadId: Long,
    val address: String,
    val isIncoming: Boolean,
    val body: String,
    val persistentFileUri: String,
    val mimeType: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface LocalMediaMessageDao {
    @Query("SELECT * FROM local_media_messages WHERE threadId = :threadId ORDER BY timestamp ASC")
    fun getMessagesForThread(threadId: Long): Flow<List<LocalMediaMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: LocalMediaMessage): Long

    @Query("DELETE FROM local_media_messages WHERE id = :id")
    suspend fun delete(id: Long)
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS scheduled_messages (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, threadId INTEGER NOT NULL, address TEXT NOT NULL, body TEXT NOT NULL, subId INTEGER NOT NULL, scheduledTimestamp INTEGER NOT NULL, createdTimestamp INTEGER NOT NULL, status TEXT NOT NULL)")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS blocked_contacts (address TEXT PRIMARY KEY NOT NULL, displayName TEXT NOT NULL, blockedTimestamp INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS message_reactions (messageId INTEGER PRIMARY KEY NOT NULL, emoji TEXT NOT NULL, timestamp INTEGER NOT NULL)")
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS local_media_messages (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, threadId INTEGER NOT NULL, address TEXT NOT NULL, isIncoming INTEGER NOT NULL, body TEXT NOT NULL, persistentFileUri TEXT NOT NULL, mimeType TEXT NOT NULL, timestamp INTEGER NOT NULL)")
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE INDEX IF NOT EXISTS index_conversation_metadata_threadId ON conversation_metadata(threadId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_conversation_metadata_isPinned ON conversation_metadata(isPinned)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_conversation_metadata_isArchived ON conversation_metadata(isArchived)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_scheduled_messages_threadId ON scheduled_messages(threadId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_scheduled_messages_status_scheduledTimestamp ON scheduled_messages(status, scheduledTimestamp)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_blocked_contacts_address ON blocked_contacts(address)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_local_media_messages_threadId ON local_media_messages(threadId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_local_media_messages_timestamp ON local_media_messages(timestamp)")
    }
}

@Database(
    entities = [
        ConversationMetadata::class,
        ScheduledMessage::class,
        BlockedContact::class,
        MessageReaction::class,
        LocalMediaMessage::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun conversationDao(): ConversationDao
    abstract fun scheduledMessageDao(): ScheduledMessageDao
    abstract fun blockedContactDao(): BlockedContactDao
    abstract fun messageReactionDao(): MessageReactionDao
    abstract fun localMediaMessageDao(): LocalMediaMessageDao
}
