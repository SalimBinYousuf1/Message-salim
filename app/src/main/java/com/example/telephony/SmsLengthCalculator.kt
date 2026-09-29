package com.example.telephony

import android.telephony.SmsMessage

data class SmsLengthInfo(
    val segmentCount: Int,
    val remainingChars: Int,
    val isUnicode: Boolean
)

object SmsLengthCalculator {
    fun calculate(text: String): SmsLengthInfo {
        if (text.isEmpty()) {
            return SmsLengthInfo(segmentCount = 1, remainingChars = 160, isUnicode = false)
        }
        return try {
            val params = SmsMessage.calculateLength(text, false)
            val msgCount = params[0]
            val codeUnitsRemaining = params[2]
            val codeUnitSize = params[3]
            val isUnicode = codeUnitSize != 1 // 1 is 7-bit, 2 is 8-bit, 3 is 16-bit UCS2
            SmsLengthInfo(
                segmentCount = msgCount,
                remainingChars = codeUnitsRemaining,
                isUnicode = isUnicode
            )
        } catch (e: Throwable) {
            // GSM 03.40 / 3GPP TS 23.040 concatenation standard
            val isUnicode = text.any { it.code > 127 }
            if (isUnicode) {
                if (text.length <= 70) {
                    SmsLengthInfo(segmentCount = 1, remainingChars = 70 - text.length, isUnicode = true)
                } else {
                    val segments = ((text.length - 1) / 67) + 1
                    val rem = (segments * 67) - text.length
                    SmsLengthInfo(segmentCount = segments, remainingChars = rem, isUnicode = true)
                }
            } else {
                if (text.length <= 160) {
                    SmsLengthInfo(segmentCount = 1, remainingChars = 160 - text.length, isUnicode = false)
                } else {
                    val segments = ((text.length - 1) / 153) + 1
                    val rem = (segments * 153) - text.length
                    SmsLengthInfo(segmentCount = segments, remainingChars = rem, isUnicode = false)
                }
            }
        }
    }
}
