package com.example.telephony

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import com.example.MainActivity
import com.example.R

object NotificationHelper {
    const val CHANNEL_MESSAGES_ID = "salim_messages_channel"
    const val KEY_TEXT_REPLY = "key_salim_text_reply"
    const val ACTION_REPLY = "com.example.ACTION_REPLY"
    const val ACTION_MARK_READ = "com.example.ACTION_MARK_READ"
    const val ACTION_COPY_OTP = "com.example.ACTION_COPY_OTP"

    const val EXTRA_THREAD_ID = "extra_thread_id"
    const val EXTRA_ADDRESS = "extra_address"
    const val EXTRA_NOTIFICATION_ID = "extra_notif_id"
    const val EXTRA_OTP_CODE = "extra_otp_code"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(
                CHANNEL_MESSAGES_ID,
                "Messages",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Incoming SMS and MMS conversations"
                enableLights(true)
                lightColor = Color.BLUE
                enableVibration(true)
                setShowBadge(true)
            }
            notificationManager?.createNotificationChannel(channel)
        }
    }

    fun showIncomingSmsNotification(
        context: Context,
        threadId: Long,
        senderAddress: String,
        senderDisplayName: String,
        messageBody: String,
        timestamp: Long,
        lockScreenPrivacy: Boolean
    ) {
        val notificationManager = context.getSystemService(NotificationManager::class.java) ?: return
        val notifId = (threadId xor senderAddress.hashCode().toLong()).toInt()

        // Tap Intent: opens Conversation in MainActivity
        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_NAV_THREAD_ID", threadId)
            putExtra("EXTRA_NAV_ADDRESS", senderAddress)
        }
        val tapPendingIntent = PendingIntent.getActivity(
            context,
            notifId,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Reply RemoteInput
        val remoteInput = RemoteInput.Builder(KEY_TEXT_REPLY)
            .setLabel("Reply…")
            .build()

        val replyIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = ACTION_REPLY
            putExtra(EXTRA_THREAD_ID, threadId)
            putExtra(EXTRA_ADDRESS, senderAddress)
            putExtra(EXTRA_NOTIFICATION_ID, notifId)
        }
        val replyPendingIntent = PendingIntent.getBroadcast(
            context,
            notifId,
            replyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        val replyAction = NotificationCompat.Action.Builder(
            android.R.drawable.ic_menu_send,
            "Reply",
            replyPendingIntent
        ).addRemoteInput(remoteInput).build()

        // Mark as Read Action
        val markReadIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = ACTION_MARK_READ
            putExtra(EXTRA_THREAD_ID, threadId)
            putExtra(EXTRA_NOTIFICATION_ID, notifId)
        }
        val markReadPendingIntent = PendingIntent.getBroadcast(
            context,
            notifId + 10000,
            markReadIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val markReadAction = NotificationCompat.Action.Builder(
            android.R.drawable.checkbox_on_background,
            "Mark as read",
            markReadPendingIntent
        ).build()

        val person = Person.Builder()
            .setName(senderDisplayName)
            .setKey(senderAddress)
            .build()

        val messagingStyle = NotificationCompat.MessagingStyle(person)
            .addMessage(messageBody, timestamp, person)

        val builder = NotificationCompat.Builder(context, CHANNEL_MESSAGES_ID)
            .setSmallIcon(R.drawable.salim_app_icon_1790181601601)
            .setStyle(messagingStyle)
            .setContentTitle(senderDisplayName)
            .setContentText(messageBody)
            .setContentIntent(tapPendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .addAction(replyAction)
            .addAction(markReadAction)

        val detectedOtp = OtpHelper.extractOtp(messageBody)
        if (!detectedOtp.isNullOrBlank()) {
            val copyOtpIntent = Intent(context, NotificationActionReceiver::class.java).apply {
                action = ACTION_COPY_OTP
                putExtra(EXTRA_OTP_CODE, detectedOtp)
                putExtra(EXTRA_THREAD_ID, threadId)
                putExtra(EXTRA_NOTIFICATION_ID, notifId)
            }
            val copyOtpPendingIntent = PendingIntent.getBroadcast(
                context,
                notifId + 20000,
                copyOtpIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val copyOtpAction = NotificationCompat.Action.Builder(
                android.R.drawable.ic_menu_save,
                "Copy $detectedOtp",
                copyOtpPendingIntent
            ).build()
            builder.addAction(copyOtpAction)
        }

        if (lockScreenPrivacy) {
            builder.setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            val publicNotification = NotificationCompat.Builder(context, CHANNEL_MESSAGES_ID)
                .setSmallIcon(R.drawable.salim_app_icon_1790181601601)
                .setContentTitle("Salim Messages")
                .setContentText("New message received")
                .setContentIntent(tapPendingIntent)
                .build()
            builder.setPublicVersion(publicNotification)
        } else {
            builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        }

        notificationManager.notify(notifId, builder.build())
    }

    fun dismissNotification(context: Context, notificationId: Int) {
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager?.cancel(notificationId)
    }
}
