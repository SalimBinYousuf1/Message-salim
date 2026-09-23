package com.example.telephony

import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.example.SalimApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

/**
 * Handles incoming SMS when Salim is the DEFAULT SMS app.
 */
class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_DELIVER_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) return

        val sender = messages[0].originatingAddress ?: "Unknown"
        val bodyBuilder = StringBuilder()
        var timestamp = System.currentTimeMillis()
        val subId = intent.getIntExtra("subscription", intent.getIntExtra(android.telephony.SubscriptionManager.EXTRA_SUBSCRIPTION_INDEX, -1))

        for (msg in messages) {
            bodyBuilder.append(msg.messageBody)
            timestamp = msg.timestampMillis
        }
        val fullBody = bodyBuilder.toString()

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Write message to Telephony.Sms.Inbox
                val threadId = SmsHelper.getOrCreateThreadId(context, sender)
                val values = ContentValues().apply {
                    put(Telephony.Sms.ADDRESS, sender)
                    put(Telephony.Sms.BODY, fullBody)
                    put(Telephony.Sms.DATE, timestamp)
                    put(Telephony.Sms.READ, 0)
                    put(Telephony.Sms.THREAD_ID, threadId)
                    put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_INBOX)
                    if (subId >= 0) {
                        put(Telephony.Sms.SUBSCRIPTION_ID, subId)
                    }
                }
                context.contentResolver.insert(Telephony.Sms.Inbox.CONTENT_URI, values)

                val (displayName, _) = SmsHelper.resolveContact(context, sender)
                val settings = (context.applicationContext as? SalimApplication)?.preferencesRepository?.settingsFlow?.firstOrNull()
                val lockScreenPrivacy = settings?.lockScreenPrivacy ?: false

                NotificationHelper.showIncomingSmsNotification(
                    context = context,
                    threadId = threadId,
                    senderAddress = sender,
                    senderDisplayName = displayName,
                    messageBody = fullBody,
                    timestamp = timestamp,
                    lockScreenPrivacy = lockScreenPrivacy
                )
            } finally {
                pendingResult.finish()
            }
        }
    }
}

/**
 * Fallback receiver for SMS_RECEIVED (for notifications if not default app or secondary broadcast).
 */
class SmsReceivedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        // If we are default SMS app, SMS_DELIVER handles it to prevent duplicates
        if (SmsHelper.isDefaultSmsApp(context)) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) return

        val sender = messages[0].originatingAddress ?: "Unknown"
        val bodyBuilder = StringBuilder()
        var timestamp = System.currentTimeMillis()

        for (msg in messages) {
            bodyBuilder.append(msg.messageBody)
            timestamp = msg.timestampMillis
        }
        val fullBody = bodyBuilder.toString()

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val threadId = SmsHelper.getOrCreateThreadId(context, sender)
                val (displayName, _) = SmsHelper.resolveContact(context, sender)
                val settings = (context.applicationContext as? SalimApplication)?.preferencesRepository?.settingsFlow?.firstOrNull()
                val lockScreenPrivacy = settings?.lockScreenPrivacy ?: false

                NotificationHelper.showIncomingSmsNotification(
                    context = context,
                    threadId = threadId,
                    senderAddress = sender,
                    senderDisplayName = displayName,
                    messageBody = fullBody,
                    timestamp = timestamp,
                    lockScreenPrivacy = lockScreenPrivacy
                )
            } finally {
                pendingResult.finish()
            }
        }
    }
}

/**
 * Handles MMS WAP push deliver.
 */
class MmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.WAP_PUSH_DELIVER_ACTION) return
        // Real MMS push deliver notification
        val mimeType = intent.type
        if (mimeType == "application/vnd.wap.mms-message") {
            // Received MMS PDU
        }
    }
}
