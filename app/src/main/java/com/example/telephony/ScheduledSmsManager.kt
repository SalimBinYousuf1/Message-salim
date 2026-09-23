package com.example.telephony

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.SalimApplication
import com.example.data.local.ScheduledMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object ScheduledSmsManager {
    const val ACTION_DISPATCH_SCHEDULED = "com.example.telephony.ACTION_DISPATCH_SCHEDULED"
    const val EXTRA_SCHEDULED_ID = "extra_scheduled_id"

    fun scheduleMessage(context: Context, scheduledMessage: ScheduledMessage) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ScheduledSmsReceiver::class.java).apply {
            action = ACTION_DISPATCH_SCHEDULED
            putExtra(EXTRA_SCHEDULED_ID, scheduledMessage.id)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            scheduledMessage.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        scheduledMessage.scheduledTimestamp,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        scheduledMessage.scheduledTimestamp,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    scheduledMessage.scheduledTimestamp,
                    pendingIntent
                )
            }
        } catch (e: Exception) {
            // fallback
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                scheduledMessage.scheduledTimestamp,
                pendingIntent
            )
        }
    }

    fun cancelSchedule(context: Context, scheduledId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ScheduledSmsReceiver::class.java).apply {
            action = ACTION_DISPATCH_SCHEDULED
            putExtra(EXTRA_SCHEDULED_ID, scheduledId)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            scheduledId.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }
}

class ScheduledSmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as? SalimApplication ?: return
        val scheduledDao = app.database.scheduledMessageDao()
        val scheduledId = intent.getLongExtra(ScheduledSmsManager.EXTRA_SCHEDULED_ID, -1L)

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (scheduledId > 0) {
                    val msg = scheduledDao.getById(scheduledId)
                    if (msg != null && msg.status == "PENDING") {
                        val success = SmsHelper.sendSms(
                            context = context,
                            destinationAddress = msg.address,
                            messageText = msg.body,
                            subscriptionId = msg.subId
                        )
                        scheduledDao.updateStatus(msg.id, if (success) "SENT" else "FAILED")
                    }
                } else {
                    // Check any pending due
                    val dueList = scheduledDao.getPendingDue(System.currentTimeMillis())
                    for (msg in dueList) {
                        val success = SmsHelper.sendSms(
                            context = context,
                            destinationAddress = msg.address,
                            messageText = msg.body,
                            subscriptionId = msg.subId
                        )
                        scheduledDao.updateStatus(msg.id, if (success) "SENT" else "FAILED")
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
