package com.example

import com.example.telephony.OtpHelper
import com.example.telephony.SmsLengthCalculator
import com.example.telephony.SpamDetector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SalimLogicUnitTest {

    @Test
    fun testSmsGsm7EncodingCalculation() {
        val standardText = "Hello Salim, this is a standard GSM7 message."
        val info = SmsLengthCalculator.calculate(standardText)

        assertFalse(info.isUnicode)
        assertEquals(1, info.segmentCount)
        assertEquals(160 - standardText.length, info.remainingChars)
    }

    @Test
    fun testSmsMultipartGsm7Calculation() {
        // 170 GSM-7 characters should split into 2 segments (153 chars per segment when concatenated)
        val longText = "A".repeat(170)
        val info = SmsLengthCalculator.calculate(longText)

        assertFalse(info.isUnicode)
        assertEquals(2, info.segmentCount)
        assertEquals(306 - 170, info.remainingChars)
    }

    @Test
    fun testSmsUnicodeCalculation() {
        val unicodeText = "Hello Salim with emoji 🚀"
        val info = SmsLengthCalculator.calculate(unicodeText)

        assertTrue(info.isUnicode)
        assertEquals(1, info.segmentCount)
        assertTrue(info.remainingChars > 0)
    }

    @Test
    fun testSmsMultipartUnicodeCalculation() {
        // Unicode limit is 70 chars for single part, 67 per part for multi-part
        // "é" is 1 char per codepoint, so 75 chars = 2 segments
        val longUnicode = "é".repeat(75)
        val info = SmsLengthCalculator.calculate(longUnicode)

        assertTrue(info.isUnicode)
        assertEquals(2, info.segmentCount)
        assertEquals((2 * 67) - 75, info.remainingChars)
    }

    @Test
    fun testSmsMultipartSurrogatePairEmojiCalculation() {
        // Emojis like 🚀 are surrogate pairs (2 UTF-16 code units per emoji)
        // 75 emojis = 150 code units, which across 67 units/segment is 3 segments
        val longEmojis = "🚀".repeat(75)
        val info = SmsLengthCalculator.calculate(longEmojis)

        assertTrue(info.isUnicode)
        assertEquals(3, info.segmentCount)
    }

    @Test
    fun testOtpExtractionStandard() {
        val snippet = "Your verification code for Google is 482910. Do not share it with anyone."
        val code = OtpHelper.extractOtp(snippet)
        assertNotNull(code)
        assertEquals("482910", code)
    }

    @Test
    fun testOtpExtractionHyphenated() {
        val snippet = "Use security code 921-304 to log into your account."
        val code = OtpHelper.extractOtp(snippet)
        assertNotNull(code)
        assertEquals("921304", code)
    }

    @Test
    fun testOtpExtractionIgnoredForNormalText() {
        val normalSnippet = "Hey, let's meet at 5pm at 123 Main Street."
        val code = OtpHelper.extractOtp(normalSnippet)
        assertNull(code)
    }

    @Test
    fun testSpamDetectorFlagsPhishingPhrases() {
        val phishingBody = "Urgent action required: Your account suspended! Verify your bank account immediately."
        val isSpam = SpamDetector.isSuspectedSpam("+1555019283", phishingBody, isKnownContact = false)
        assertTrue("Should detect phishing message from unknown sender", isSpam)
    }

    @Test
    fun testSpamDetectorFlagsSuspiciousShortenedUrl() {
        val shortUrlBody = "Claim your exclusive discount here: https://bit.ly/3xSample"
        val isSpam = SpamDetector.isSuspectedSpam("+1555019283", shortUrlBody, isKnownContact = false)
        assertTrue("Should detect shortened URL from unknown sender", isSpam)
    }

    @Test
    fun testSpamDetectorExemptsKnownContacts() {
        val message = "Claim your prize now! bit.ly/test"
        val isSpam = SpamDetector.isSuspectedSpam("+1555019283", message, isKnownContact = true)
        assertFalse("Known contacts should never be flagged as suspected spam", isSpam)
    }

    @Test
    fun testSpamDetectorAllowsSafeMessages() {
        val safeMessage = "Hey Sarah, are we still meeting for lunch tomorrow?"
        val isSpam = SpamDetector.isSuspectedSpam("+1555019283", safeMessage, isKnownContact = false)
        assertFalse("Normal conversational message should not be flagged", isSpam)
    }

    @Test
    fun testTransactionAndOtpClassification() {
        assertTrue(OtpHelper.isTransactionOrOtp("CHASE", "Your balance was updated."))
        assertTrue(OtpHelper.isTransactionOrOtp("12345", "Verification code 192831"))
        assertFalse(OtpHelper.isTransactionOrOtp("+15551234567", "Hello how are you doing?"))
    }
}
