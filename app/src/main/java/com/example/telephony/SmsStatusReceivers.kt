package com.example.telephony

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Telephony

/**
 * Updates status of SMS when sent successfully or failed.
 */
class SmsSentReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val uriString = intent.getStringExtra(SmsHelper.EXTRA_MESSAGE_URI) ?: return
        val messageUri = Uri.parse(uriString)

        val resultCode = resultCode
        val values = ContentValues()

        if (resultCode == Activity.RESULT_OK) {
            values.put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_SENT)
            values.put(Telephony.Sms.STATUS, Telephony.Sms.STATUS_NONE)
        } else {
            values.put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_FAILED)
            values.put(Telephony.Sms.STATUS, Telephony.Sms.STATUS_FAILED)
        }

        try {
            context.contentResolver.update(messageUri, values, null, null)
        } catch (e: Exception) {
            // ignore
        }
    }
}

/**
 * Updates status of SMS when delivered to the recipient.
 */
class SmsDeliveredReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val uriString = intent.getStringExtra(SmsHelper.EXTRA_MESSAGE_URI) ?: return
        val messageUri = Uri.parse(uriString)

        val values = ContentValues().apply {
            put(Telephony.Sms.STATUS, Telephony.Sms.STATUS_COMPLETE)
        }

        try {
            context.contentResolver.update(messageUri, values, null, null)
        } catch (e: Exception) {
            // ignore
        }
    }
}
