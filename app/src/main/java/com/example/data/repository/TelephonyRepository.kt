package com.example.data.repository

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Telephony
import com.example.data.local.ConversationDao
import com.example.data.local.ConversationMetadata
import com.example.data.model.Conversation
import com.example.data.model.Message
import com.example.data.model.SimCardInfo
import com.example.telephony.SmsHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

class TelephonyRepository(
    private val context: Context,
    private val conversationDao: ConversationDao
) {

    /**
     * Emits a Unit signal whenever telephony SMS or MMS content changes.
     */
    private val telephonyContentChangeFlow: Flow<Unit> = callbackFlow {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                trySend(Unit)
            }
        }

        try {
            context.contentResolver.registerContentObserver(
                Telephony.Sms.CONTENT_URI,
                true,
                observer
            )
            context.contentResolver.registerContentObserver(
                Telephony.MmsSms.CONTENT_CONVERSATIONS_URI,
                true,
                observer
            )
        } catch (e: Exception) {
            // In case of permission delay
        }

        // Initial trigger
        trySend(Unit)

        awaitClose {
            try {
                context.contentResolver.unregisterContentObserver(observer)
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    /**
     * Flow of all active conversations, merged with Room metadata (pinned, archived, drafts).
     */
    val conversationsFlow: Flow<List<Conversation>> = combine(
        telephonyContentChangeFlow,
        conversationDao.getAllMetadata()
    ) { _, metadataList ->
        val rawConversations = SmsHelper.queryConversations(context)
        val metadataMap = metadataList.associateBy { it.threadId }

        rawConversations.map { conv ->
            val meta = metadataMap[conv.threadId]
            conv.copy(
                isPinned = meta?.isPinned ?: false,
                isArchived = meta?.isArchived ?: false,
                displayName = meta?.customName ?: conv.displayName,
                draft = meta?.draft
            )
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Flow of messages for a specific conversation thread.
     */
    fun getMessagesFlow(threadId: Long): Flow<List<Message>> = callbackFlow {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                trySend(Unit)
            }
        }

        try {
            context.contentResolver.registerContentObserver(
                Telephony.Sms.CONTENT_URI,
                true,
                observer
            )
        } catch (e: Exception) {
            // ignore
        }

        // Initial emission
        trySend(Unit)

        awaitClose {
            try {
                context.contentResolver.unregisterContentObserver(observer)
            } catch (e: Exception) {
                // ignore
            }
        }
    }.combine(conversationDao.getAllMetadata()) { _, _ ->
        SmsHelper.queryMessages(context, threadId)
    }.flowOn(Dispatchers.IO)

    suspend fun sendMessage(
        threadId: Long,
        destinationAddress: String,
        messageText: String,
        subId: Int = -1
    ): Boolean = withContext(Dispatchers.IO) {
        val success = SmsHelper.sendSms(context, destinationAddress, messageText, subId)
        if (success && threadId > 0) {
            // Clear draft
            conversationDao.saveDraft(threadId, null)
        }
        success
    }

    suspend fun markThreadAsRead(threadId: Long) = withContext(Dispatchers.IO) {
        SmsHelper.markThreadAsRead(context, threadId)
    }

    suspend fun deleteMessage(messageId: Long): Boolean = withContext(Dispatchers.IO) {
        SmsHelper.deleteMessage(context, messageId)
    }

    suspend fun deleteThread(threadId: Long): Boolean = withContext(Dispatchers.IO) {
        val deleted = SmsHelper.deleteThread(context, threadId)
        conversationDao.deleteMetadata(threadId)
        deleted
    }

    suspend fun setPinned(threadId: Long, isPinned: Boolean) = withContext(Dispatchers.IO) {
        val current = conversationDao.getMetadata(threadId)
        if (current == null) {
            conversationDao.insertOrUpdate(ConversationMetadata(threadId = threadId, isPinned = isPinned))
        } else {
            conversationDao.setPinned(threadId, isPinned)
        }
    }

    suspend fun setArchived(threadId: Long, isArchived: Boolean) = withContext(Dispatchers.IO) {
        val current = conversationDao.getMetadata(threadId)
        if (current == null) {
            conversationDao.insertOrUpdate(ConversationMetadata(threadId = threadId, isArchived = isArchived))
        } else {
            conversationDao.setArchived(threadId, isArchived)
        }
    }

    suspend fun saveDraft(threadId: Long, draft: String?) = withContext(Dispatchers.IO) {
        val current = conversationDao.getMetadata(threadId)
        if (current == null) {
            conversationDao.insertOrUpdate(ConversationMetadata(threadId = threadId, draft = draft))
        } else {
            conversationDao.saveDraft(threadId, draft)
        }
    }

    suspend fun getDraft(threadId: Long): String? = withContext(Dispatchers.IO) {
        conversationDao.getMetadata(threadId)?.draft
    }

    fun getAvailableSims(): List<SimCardInfo> {
        return SmsHelper.getAvailableSims(context)
    }

    suspend fun getOrCreateThreadId(address: String): Long {
        return SmsHelper.getOrCreateThreadId(context, address)
    }
}
