package com.example.data.model

import android.net.Uri

enum class MessageStatus {
    SENDING,
    SENT,
    DELIVERED,
    FAILED
}

data class Conversation(
    val threadId: Long,
    val address: String,
    val displayName: String,
    val photoUri: String? = null,
    val snippet: String = "",
    val date: Long = 0L,
    val unreadCount: Int = 0,
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val hasMms: Boolean = false,
    val draft: String? = null
)

data class Message(
    val id: Long,
    val threadId: Long,
    val address: String,
    val body: String,
    val date: Long,
    val isIncoming: Boolean,
    val status: MessageStatus = MessageStatus.SENT,
    val isMms: Boolean = false,
    val mediaUri: Uri? = null,
    val mediaMimeType: String? = null,
    val subId: Int = -1
)

data class ContactItem(
    val id: Long,
    val lookupKey: String,
    val displayName: String,
    val phoneNumber: String,
    val photoUri: String? = null
)

data class SimCardInfo(
    val subscriptionId: Int,
    val displayName: String,
    val carrierName: String,
    val slotIndex: Int
)
