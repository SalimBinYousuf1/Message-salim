package com.example.telephony

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.RemoteInput
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val notifId = intent.getIntExtra(NotificationHelper.EXTRA_NOTIFICATION_ID, -1)
        val threadId = intent.getLongExtra(NotificationHelper.EXTRA_THREAD_ID, -1L)
        val address = intent.getStringExtra(NotificationHelper.EXTRA_ADDRESS) ?: ""

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (action) {
                    NotificationHelper.ACTION_REPLY -> {
                        val results = RemoteInput.getResultsFromIntent(intent)
                        val replyText = results?.getCharSequence(NotificationHelper.KEY_TEXT_REPLY)?.toString()
                        if (!replyText.isNullOrBlank() && address.isNotBlank()) {
                            SmsHelper.sendSms(context, address, replyText)
                            if (threadId > 0) {
                                SmsHelper.markThreadAsRead(context, threadId)
                            }
                        }
                        if (notifId != -1) {
                            NotificationHelper.dismissNotification(context, notifId)
                        }
                    }

                    NotificationHelper.ACTION_MARK_READ -> {
                        if (threadId > 0) {
                            SmsHelper.markThreadAsRead(context, threadId)
                        }
                        if (notifId != -1) {
                            NotificationHelper.dismissNotification(context, notifId)
                        }
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
