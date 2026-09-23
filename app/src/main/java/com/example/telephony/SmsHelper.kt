package com.example.telephony

import android.app.PendingIntent
import android.app.role.RoleManager
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.provider.Telephony
import android.telephony.SmsManager
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import androidx.core.content.ContextCompat
import com.example.data.model.Conversation
import com.example.data.model.Message
import com.example.data.model.MessageStatus
import com.example.data.model.SimCardInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

object SmsHelper {

    const val ACTION_SMS_SENT = "com.example.SMS_SENT"
    const val ACTION_SMS_DELIVERED = "com.example.SMS_DELIVERED"
    const val EXTRA_MESSAGE_URI = "extra_message_uri"

    /**
     * Checks if Salim is the default SMS application.
     */
    fun isDefaultSmsApp(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            roleManager?.isRoleHeld(RoleManager.ROLE_SMS) == true
        } else {
            val defaultPackage = Telephony.Sms.getDefaultSmsPackage(context)
            defaultPackage != null && defaultPackage == context.packageName
        }
    }

    /**
     * Returns intent to request becoming the default SMS app.
     */
    fun getRequestDefaultSmsAppIntent(context: Context): Intent? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            roleManager?.createRequestRoleIntent(RoleManager.ROLE_SMS)
        } else {
            Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT).apply {
                putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, context.packageName)
            }
        }
    }

    /**
     * Checks required telephony and contacts permissions.
     */
    fun hasRequiredPermissions(context: Context): Boolean {
        val readSms = ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
        val sendSms = ContextCompat.checkSelfPermission(context, android.Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED
        val readContacts = ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
        return readSms && sendSms && readContacts
    }

    /**
     * Query all available SIM cards on the device.
     */
    fun getAvailableSims(context: Context): List<SimCardInfo> {
        val list = mutableListOf<SimCardInfo>()
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            return list
        }
        try {
            val subscriptionManager = context.getSystemService(SubscriptionManager::class.java)
            val subList: List<SubscriptionInfo>? = subscriptionManager?.activeSubscriptionInfoList
            subList?.forEach { sub ->
                list.add(
                    SimCardInfo(
                        subscriptionId = sub.subscriptionId,
                        displayName = sub.displayName?.toString() ?: "SIM ${sub.simSlotIndex + 1}",
                        carrierName = sub.carrierName?.toString() ?: "Carrier",
                        slotIndex = sub.simSlotIndex
                    )
                )
            }
        } catch (e: Exception) {
            // Permission or device limitation
        }
        return list
    }

    /**
     * Resolve contact display name and photo URI from phone number.
     */
    fun resolveContact(context: Context, phoneNumber: String): Pair<String, String?> {
        if (phoneNumber.isBlank() ||
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED
        ) {
            return Pair(phoneNumber, null)
        }

        val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber))
        val projection = arrayOf(
            ContactsContract.PhoneLookup.DISPLAY_NAME,
            ContactsContract.PhoneLookup.PHOTO_URI,
            ContactsContract.PhoneLookup.PHOTO_THUMBNAIL_URI
        )

        try {
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIdx = cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)
                    val photoIdx = cursor.getColumnIndex(ContactsContract.PhoneLookup.PHOTO_URI)
                    val thumbIdx = cursor.getColumnIndex(ContactsContract.PhoneLookup.PHOTO_THUMBNAIL_URI)

                    val name = if (nameIdx != -1) cursor.getString(nameIdx) else null
                    val photo = if (photoIdx != -1) cursor.getString(photoIdx) else null
                    val thumb = if (thumbIdx != -1) cursor.getString(thumbIdx) else null

                    if (!name.isNullOrBlank()) {
                        return Pair(name, photo ?: thumb)
                    }
                }
            }
        } catch (e: Exception) {
            // ignore
        }
        return Pair(phoneNumber, null)
    }

    /**
     * Query all conversations from telephony provider.
     */
    suspend fun queryConversations(context: Context): List<Conversation> = withContext(Dispatchers.IO) {
        val result = mutableListOf<Conversation>()
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            return@withContext result
        }

        // Use Telephony.MmsSms.CONTENT_CONVERSATIONS_URI with simple=true or fallback to Telephony.Sms.Conversations.CONTENT_URI
        val uri = Telephony.MmsSms.CONTENT_CONVERSATIONS_URI
        val projection = arrayOf(
            "thread_id",
            "snippet",
            "date",
            "read",
            "msg_count"
        )

        val seenThreads = mutableSetOf<Long>()

        try {
            context.contentResolver.query(uri, projection, null, null, "date DESC")?.use { cursor ->
                val threadIdIdx = cursor.getColumnIndex("thread_id")
                val snippetIdx = cursor.getColumnIndex("snippet")
                val dateIdx = cursor.getColumnIndex("date")
                val readIdx = cursor.getColumnIndex("read")

                while (cursor.moveToNext()) {
                    val threadId = if (threadIdIdx != -1) cursor.getLong(threadIdIdx) else -1L
                    if (threadId <= 0 || seenThreads.contains(threadId)) continue
                    seenThreads.add(threadId)

                    val snippet = if (snippetIdx != -1) cursor.getString(snippetIdx) ?: "" else ""
                    val date = if (dateIdx != -1) cursor.getLong(dateIdx) else System.currentTimeMillis()
                    val isRead = if (readIdx != -1) cursor.getInt(readIdx) == 1 else true

                    val address = queryAddressForThread(context, threadId)
                    val (displayName, photoUri) = resolveContact(context, address)
                    val unreadCount = if (!isRead) queryUnreadCountForThread(context, threadId) else 0

                    result.add(
                        Conversation(
                            threadId = threadId,
                            address = address,
                            displayName = displayName,
                            photoUri = photoUri,
                            snippet = snippet,
                            date = if (date > 100000000000L) date else date * 1000L,
                            unreadCount = unreadCount,
                            hasMms = false
                        )
                    )
                }
            }
        } catch (e: Exception) {
            // Fallback: Query directly from Telephony.Sms
            return@withContext queryConversationsFromSmsTable(context)
        }

        if (result.isEmpty()) {
            return@withContext queryConversationsFromSmsTable(context)
        }

        result
    }

    private fun queryConversationsFromSmsTable(context: Context): List<Conversation> {
        val result = mutableListOf<Conversation>()
        val uri = Telephony.Sms.CONTENT_URI
        val projection = arrayOf(
            Telephony.Sms.THREAD_ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
            Telephony.Sms.READ
        )

        val map = linkedMapOf<Long, Conversation>()

        try {
            context.contentResolver.query(uri, projection, null, null, "date DESC")?.use { cursor ->
                val threadIdIdx = cursor.getColumnIndex(Telephony.Sms.THREAD_ID)
                val addressIdx = cursor.getColumnIndex(Telephony.Sms.ADDRESS)
                val bodyIdx = cursor.getColumnIndex(Telephony.Sms.BODY)
                val dateIdx = cursor.getColumnIndex(Telephony.Sms.DATE)
                val readIdx = cursor.getColumnIndex(Telephony.Sms.READ)

                while (cursor.moveToNext()) {
                    val threadId = if (threadIdIdx != -1) cursor.getLong(threadIdIdx) else -1L
                    if (threadId <= 0) continue

                    val address = if (addressIdx != -1) cursor.getString(addressIdx) ?: "Unknown" else "Unknown"
                    val body = if (bodyIdx != -1) cursor.getString(bodyIdx) ?: "" else ""
                    val date = if (dateIdx != -1) cursor.getLong(dateIdx) else System.currentTimeMillis()
                    val isRead = if (readIdx != -1) cursor.getInt(readIdx) == 1 else true

                    if (!map.containsKey(threadId)) {
                        val (displayName, photoUri) = resolveContact(context, address)
                        map[threadId] = Conversation(
                            threadId = threadId,
                            address = address,
                            displayName = displayName,
                            photoUri = photoUri,
                            snippet = body,
                            date = if (date > 100000000000L) date else date * 1000L,
                            unreadCount = if (!isRead) 1 else 0
                        )
                    } else if (!isRead) {
                        val existing = map[threadId]!!
                        map[threadId] = existing.copy(unreadCount = existing.unreadCount + 1)
                    }
                }
            }
        } catch (e: Exception) {
            // ignore
        }
        return map.values.toList()
    }

    private fun queryAddressForThread(context: Context, threadId: Long): String {
        val uri = Telephony.Sms.CONTENT_URI
        val projection = arrayOf(Telephony.Sms.ADDRESS)
        try {
            context.contentResolver.query(
                uri,
                projection,
                "${Telephony.Sms.THREAD_ID} = ?",
                arrayOf(threadId.toString()),
                "date DESC LIMIT 1"
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(Telephony.Sms.ADDRESS)
                    if (idx != -1) {
                        val addr = cursor.getString(idx)
                        if (!addr.isNullOrBlank()) return addr
                    }
                }
            }
        } catch (e: Exception) {
            // ignore
        }
        return "Unknown"
    }

    private fun queryUnreadCountForThread(context: Context, threadId: Long): Int {
        val uri = Telephony.Sms.CONTENT_URI
        val projection = arrayOf("COUNT(*)")
        try {
            context.contentResolver.query(
                uri,
                projection,
                "${Telephony.Sms.THREAD_ID} = ? AND ${Telephony.Sms.READ} = 0",
                arrayOf(threadId.toString()),
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    return cursor.getInt(0)
                }
            }
        } catch (e: Exception) {
            // ignore
        }
        return 0
    }

    /**
     * Query all messages for a specific thread from both SMS and MMS.
     */
    suspend fun queryMessages(context: Context, threadId: Long): List<Message> = withContext(Dispatchers.IO) {
        val messages = mutableListOf<Message>()
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            return@withContext messages
        }

        // Query SMS
        val smsUri = Telephony.Sms.CONTENT_URI
        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.THREAD_ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
            Telephony.Sms.TYPE,
            Telephony.Sms.STATUS,
            Telephony.Sms.SUBSCRIPTION_ID
        )

        try {
            context.contentResolver.query(
                smsUri,
                projection,
                "${Telephony.Sms.THREAD_ID} = ?",
                arrayOf(threadId.toString()),
                "date ASC"
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndex(Telephony.Sms._ID)
                val threadIdIdx = cursor.getColumnIndex(Telephony.Sms.THREAD_ID)
                val addressIdx = cursor.getColumnIndex(Telephony.Sms.ADDRESS)
                val bodyIdx = cursor.getColumnIndex(Telephony.Sms.BODY)
                val dateIdx = cursor.getColumnIndex(Telephony.Sms.DATE)
                val typeIdx = cursor.getColumnIndex(Telephony.Sms.TYPE)
                val statusIdx = cursor.getColumnIndex(Telephony.Sms.STATUS)
                val subIdIdx = cursor.getColumnIndex(Telephony.Sms.SUBSCRIPTION_ID)

                while (cursor.moveToNext()) {
                    val id = if (idIdx != -1) cursor.getLong(idIdx) else 0L
                    val tId = if (threadIdIdx != -1) cursor.getLong(threadIdIdx) else threadId
                    val address = if (addressIdx != -1) cursor.getString(addressIdx) ?: "" else ""
                    val body = if (bodyIdx != -1) cursor.getString(bodyIdx) ?: "" else ""
                    val date = if (dateIdx != -1) cursor.getLong(dateIdx) else 0L
                    val type = if (typeIdx != -1) cursor.getInt(typeIdx) else Telephony.Sms.MESSAGE_TYPE_INBOX
                    val statusVal = if (statusIdx != -1) cursor.getInt(statusIdx) else -1
                    val subId = if (subIdIdx != -1) cursor.getInt(subIdIdx) else -1

                    val isIncoming = type == Telephony.Sms.MESSAGE_TYPE_INBOX
                    val status = when {
                        isIncoming -> MessageStatus.DELIVERED
                        type == Telephony.Sms.MESSAGE_TYPE_OUTBOX || type == Telephony.Sms.MESSAGE_TYPE_QUEUED -> MessageStatus.SENDING
                        type == Telephony.Sms.MESSAGE_TYPE_FAILED -> MessageStatus.FAILED
                        statusVal == Telephony.Sms.STATUS_COMPLETE -> MessageStatus.DELIVERED
                        else -> MessageStatus.SENT
                    }

                    messages.add(
                        Message(
                            id = id,
                            threadId = tId,
                            address = address,
                            body = body,
                            date = if (date > 100000000000L) date else date * 1000L,
                            isIncoming = isIncoming,
                            status = status,
                            isMms = false,
                            subId = subId
                        )
                    )
                }
            }
        } catch (e: Exception) {
            // ignore
        }

        // Also query MMS for this thread if any
        try {
            queryMmsMessages(context, threadId, messages)
        } catch (e: Exception) {
            // MMS might not be present or supported
        }

        messages.sortedBy { it.date }
    }

    private fun queryMmsMessages(context: Context, threadId: Long, list: MutableList<Message>) {
        val mmsUri = Telephony.Mms.CONTENT_URI
        val projection = arrayOf(
            Telephony.Mms._ID,
            Telephony.Mms.THREAD_ID,
            Telephony.Mms.DATE,
            Telephony.Mms.MESSAGE_BOX
        )

        context.contentResolver.query(
            mmsUri,
            projection,
            "${Telephony.Mms.THREAD_ID} = ?",
            arrayOf(threadId.toString()),
            "date ASC"
        )?.use { cursor ->
            val idIdx = cursor.getColumnIndex(Telephony.Mms._ID)
            val tIdIdx = cursor.getColumnIndex(Telephony.Mms.THREAD_ID)
            val dateIdx = cursor.getColumnIndex(Telephony.Mms.DATE)
            val boxIdx = cursor.getColumnIndex(Telephony.Mms.MESSAGE_BOX)

            while (cursor.moveToNext()) {
                val mmsId = if (idIdx != -1) cursor.getLong(idIdx) else continue
                val tId = if (tIdIdx != -1) cursor.getLong(tIdIdx) else threadId
                val date = if (dateIdx != -1) cursor.getLong(dateIdx) * 1000L else System.currentTimeMillis()
                val box = if (boxIdx != -1) cursor.getInt(boxIdx) else Telephony.Mms.MESSAGE_BOX_INBOX
                val isIncoming = box == Telephony.Mms.MESSAGE_BOX_INBOX

                val (body, mediaUri, mediaMime) = queryMmsPart(context, mmsId)

                list.add(
                    Message(
                        id = mmsId + 100000000L, // offset to prevent ID collision with SMS
                        threadId = tId,
                        address = "",
                        body = body ?: "",
                        date = date,
                        isIncoming = isIncoming,
                        status = MessageStatus.SENT,
                        isMms = true,
                        mediaUri = mediaUri,
                        mediaMimeType = mediaMime
                    )
                )
            }
        }
    }

    private fun queryMmsPart(context: Context, mmsId: Long): Triple<String?, Uri?, String?> {
        val partUri = Uri.parse("content://mms/part")
        var text: String? = null
        var mediaUri: Uri? = null
        var mediaMime: String? = null

        context.contentResolver.query(
            partUri,
            arrayOf("_id", "mid", "ct", "text", "_data"),
            "mid = ?",
            arrayOf(mmsId.toString()),
            null
        )?.use { cursor ->
            val idIdx = cursor.getColumnIndex("_id")
            val ctIdx = cursor.getColumnIndex("ct")
            val textIdx = cursor.getColumnIndex("text")

            while (cursor.moveToNext()) {
                val partId = if (idIdx != -1) cursor.getLong(idIdx) else continue
                val contentType = if (ctIdx != -1) cursor.getString(ctIdx) else null

                if (contentType == "text/plain") {
                    val data = if (textIdx != -1) cursor.getString(textIdx) else null
                    if (data != null) {
                        text = data
                    } else {
                        // Read from stream
                        val contentUri = ContentUris.withAppendedId(partUri, partId)
                        text = readInputStreamAsString(context, contentUri)
                    }
                } else if (contentType?.startsWith("image/") == true ||
                    contentType?.startsWith("video/") == true ||
                    contentType?.startsWith("audio/") == true
                ) {
                    mediaUri = ContentUris.withAppendedId(partUri, partId)
                    mediaMime = contentType
                }
            }
        }
        return Triple(text, mediaUri, mediaMime)
    }

    private fun readInputStreamAsString(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BufferedReader(InputStreamReader(stream)).readText()
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Send real SMS through Android SmsManager and log to ContentProvider.
     */
    suspend fun sendSms(
        context: Context,
        destinationAddress: String,
        messageText: String,
        subscriptionId: Int = -1
    ): Boolean = withContext(Dispatchers.IO) {
        if (destinationAddress.isBlank() || messageText.isBlank()) return@withContext false

        try {
            val smsManager: SmsManager = if (subscriptionId >= 0) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    context.getSystemService(SmsManager::class.java).createForSubscriptionId(subscriptionId)
                } else {
                    @Suppress("DEPRECATION")
                    SmsManager.getSmsManagerForSubscriptionId(subscriptionId)
                }
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    context.getSystemService(SmsManager::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    SmsManager.getDefault()
                }
            }

            // Insert into Telephony.Sms.Sent
            val values = ContentValues().apply {
                put(Telephony.Sms.ADDRESS, destinationAddress)
                put(Telephony.Sms.BODY, messageText)
                put(Telephony.Sms.DATE, System.currentTimeMillis())
                put(Telephony.Sms.READ, 1)
                put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_SENT)
                if (subscriptionId >= 0) {
                    put(Telephony.Sms.SUBSCRIPTION_ID, subscriptionId)
                }
            }

            val insertedUri = try {
                context.contentResolver.insert(Telephony.Sms.Sent.CONTENT_URI, values)
            } catch (e: Exception) {
                null
            }

            // Sent PendingIntent
            val sentIntent = Intent(ACTION_SMS_SENT).apply {
                putExtra(EXTRA_MESSAGE_URI, insertedUri?.toString())
            }
            val sentPendingIntent = PendingIntent.getBroadcast(
                context,
                System.currentTimeMillis().toInt(),
                sentIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Delivered PendingIntent
            val deliveredIntent = Intent(ACTION_SMS_DELIVERED).apply {
                putExtra(EXTRA_MESSAGE_URI, insertedUri?.toString())
            }
            val deliveredPendingIntent = PendingIntent.getBroadcast(
                context,
                (System.currentTimeMillis() + 1).toInt(),
                deliveredIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Segment / concatenate SMS if text exceeds 160 characters
            val parts = smsManager.divideMessage(messageText)
            if (parts.size > 1) {
                val sentIntents = ArrayList<PendingIntent>(parts.size).apply {
                    for (i in parts.indices) add(sentPendingIntent)
                }
                val deliveredIntents = ArrayList<PendingIntent>(parts.size).apply {
                    for (i in parts.indices) add(deliveredPendingIntent)
                }
                smsManager.sendMultipartTextMessage(
                    destinationAddress,
                    null,
                    parts,
                    sentIntents,
                    deliveredIntents
                )
            } else {
                smsManager.sendTextMessage(
                    destinationAddress,
                    null,
                    messageText,
                    sentPendingIntent,
                    deliveredPendingIntent
                )
            }
            return@withContext true
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        }
    }

    /**
     * Mark a conversation thread as read.
     */
    suspend fun markThreadAsRead(context: Context, threadId: Long) = withContext(Dispatchers.IO) {
        try {
            val values = ContentValues().apply {
                put(Telephony.Sms.READ, 1)
                put(Telephony.Sms.SEEN, 1)
            }
            context.contentResolver.update(
                Telephony.Sms.CONTENT_URI,
                values,
                "${Telephony.Sms.THREAD_ID} = ? AND ${Telephony.Sms.READ} = 0",
                arrayOf(threadId.toString())
            )
        } catch (e: Exception) {
            // ignore
        }
    }

    /**
     * Delete an individual message by ID.
     */
    suspend fun deleteMessage(context: Context, messageId: Long): Boolean = withContext(Dispatchers.IO) {
        try {
            val deleted = context.contentResolver.delete(
                Telephony.Sms.CONTENT_URI,
                "${Telephony.Sms._ID} = ?",
                arrayOf(messageId.toString())
            )
            return@withContext deleted > 0
        } catch (e: Exception) {
            return@withContext false
        }
    }

    /**
     * Delete an entire conversation thread.
     */
    suspend fun deleteThread(context: Context, threadId: Long): Boolean = withContext(Dispatchers.IO) {
        try {
            val uri = ContentUris.withAppendedId(Telephony.Threads.CONTENT_URI, threadId)
            val deleted = context.contentResolver.delete(uri, null, null)
            if (deleted > 0) return@withContext true

            // Fallback: delete all SMS for this threadId
            val smsDeleted = context.contentResolver.delete(
                Telephony.Sms.CONTENT_URI,
                "${Telephony.Sms.THREAD_ID} = ?",
                arrayOf(threadId.toString())
            )
            return@withContext smsDeleted > 0
        } catch (e: Exception) {
            return@withContext false
        }
    }

    /**
     * Resolve threadId for an address if creating a new conversation.
     */
    suspend fun getOrCreateThreadId(context: Context, recipient: String): Long = withContext(Dispatchers.IO) {
        try {
            return@withContext Telephony.Threads.getOrCreateThreadId(context, recipient)
        } catch (e: Exception) {
            return@withContext recipient.hashCode().toLong()
        }
    }
}
