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
        } catch (e: Exception) {
            val isUnicode = text.any { it.code > 127 }
            val limit = if (isUnicode) 70 else 160
            val segments = (text.length / limit) + 1
            val rem = limit - (text.length % limit)
            SmsLengthInfo(segmentCount = segments, remainingChars = rem, isUnicode = isUnicode)
        }
    }
}
