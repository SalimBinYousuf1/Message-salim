package com.example.telephony

import java.util.regex.Pattern

object OtpHelper {
    // Matches common OTP patterns:
    // e.g. "is 123456", "code: 948201", "OTP: 4928", "G-123456"
    private val OTP_PATTERNS = listOf(
        Pattern.compile("""(?i)(?:code|otp|passcode|secret|verification code|pin)\D{0,10}(\b\d{4,8}\b)"""),
        Pattern.compile("""\b[A-Z0-9]{1,3}-\d{4,8}\b"""),
        Pattern.compile("""(?i)\b(\d{4,8})\b(?=.*(?:code|verification|verify|authenticate|login))"""),
        Pattern.compile("""(?i)(?:is|:)\s*(\b\d{4,8}\b)""")
    )

    fun extractOtp(text: String): String? {
        if (text.isBlank()) return null
        for (pattern in OTP_PATTERNS) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                val group = if (matcher.groupCount() >= 1) matcher.group(1) else matcher.group(0)
                if (!group.isNullOrBlank() && group.length in 4..8) {
                    return group.trim()
                }
            }
        }
        return null
    }

    fun isTransactionOrOtp(address: String, snippet: String): Boolean {
        // Alphanumeric sender like "CHASE", "GOOGLE", "AMAZON", or shortcode 4-6 digits without +
        val isShortcode = address.matches(Regex("""^\d{4,6}$"""))
        val isAlphaSender = address.matches(Regex("""^[a-zA-Z0-9\-_]{3,11}$""")) && !address.matches(Regex("""^\d+$"""))
        val hasOtpKeyword = extractOtp(snippet) != null ||
                snippet.contains("code", ignoreCase = true) ||
                snippet.contains("otp", ignoreCase = true) ||
                snippet.contains("account", ignoreCase = true) ||
                snippet.contains("alert", ignoreCase = true) ||
                snippet.contains("bank", ignoreCase = true) ||
                snippet.contains("credited", ignoreCase = true) ||
                snippet.contains("debited", ignoreCase = true)
        return isShortcode || isAlphaSender || hasOtpKeyword
    }
}
