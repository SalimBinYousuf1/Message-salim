package com.example.telephony

import android.app.Service
import android.content.Intent
import android.net.Uri
import android.os.IBinder
import android.telephony.TelephonyManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Headless SMS send service: mandatory for Default SMS App compliance on Android.
 * Responds to RESPOND_VIA_MESSAGE (e.g. Reject incoming call with SMS).
 */
class HeadlessSmsSendService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        val action = intent.action
        if (TelephonyManager.ACTION_RESPOND_VIA_MESSAGE == action) {
            val extras = intent.extras
            if (extras != null) {
                val message = extras.getString(Intent.EXTRA_TEXT)
                val intentUri: Uri? = intent.data
                val recipient = intentUri?.schemeSpecificPart?.substringBefore('?')

                if (!recipient.isNullOrBlank() && !message.isNullOrBlank()) {
                    CoroutineScope(Dispatchers.IO).launch {
                        SmsHelper.sendSms(applicationContext, recipient, message)
                        stopSelf(startId)
                    }
                    return START_REDELIVER_INTENT
                }
            }
        }
        stopSelf(startId)
        return START_NOT_STICKY
    }
}
