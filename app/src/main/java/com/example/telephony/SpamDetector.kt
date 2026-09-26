package com.example.telephony

import java.util.regex.Pattern

object SpamDetector {

    private val SUSPICIOUS_PHRASES = listOf(
        "claim your prize",
        "lottery winner",
        "won a prize",
        "free gift card",
        "urgent action required",
        "account suspended",
        "verify your bank account",
        "unusual login activity",
        "double your money",
        "guaranteed return",
        "crypto profit",
        "deposit now",
        "click link to claim",
        "congratulations! you have won",
        "wire transfer immediately"
    )

    private val SUSPICIOUS_URL_PATTERN = Pattern.compile(
        """(?i)\b(https?://)?([a-z0-9-]+\.)*(bit\.ly|tinyurl\.com|t\.co|is\.gd|cutt\.ly|ow\.ly|[a-z0-9-]+\.(xyz|top|work|click|loan|gq|cf|tk))\b"""
    )

    fun isSuspectedSpam(address: String, body: String, isKnownContact: Boolean): Boolean {
        if (isKnownContact) return false
        if (body.isBlank()) return false

        val lowerBody = body.lowercase()

        // 1. Check phishing / scam phrases
        for (phrase in SUSPICIOUS_PHRASES) {
            if (lowerBody.contains(phrase)) {
                return true
            }
        }

        // 2. Check suspicious link shorteners or dangerous TLDs from unknown senders
        if (SUSPICIOUS_URL_PATTERN.matcher(body).find()) {
            return true
        }

        return false
    }

    fun getSpamWarningMessage(body: String): String {
        val lower = body.lowercase()
        return when {
            lower.contains("account suspended") || lower.contains("verify") || lower.contains("bank") ->
                "Suspected Phishing: This message asks for urgent verification or credentials. Do not click links or share passwords."
            lower.contains("prize") || lower.contains("winner") || lower.contains("lottery") || lower.contains("reward") ->
                "Suspected Lottery Scam: Unsolicited prize claims often request fees or personal data."
            SUSPICIOUS_URL_PATTERN.matcher(body).find() ->
                "Suspicious Link Detected: Contains shortened or unverified link from an unknown sender."
            else ->
                "Suspected Spam: The sender is not in your contacts and this message matches common spam patterns."
        }
    }
}
