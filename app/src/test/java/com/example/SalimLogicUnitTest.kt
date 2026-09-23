package com.example

import com.example.telephony.OtpHelper
import com.example.telephony.SmsLengthCalculator
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
        assertEquals(160, info.maxSegmentLength)
        assertEquals(160 - standardText.length, info.remainingInCurrentSegment)
    }

    @Test
    fun testSmsUnicodeCalculation() {
        val unicodeText = "Hello Salim with emoji 🚀"
        val info = SmsLengthCalculator.calculate(unicodeText)

        assertTrue(info.isUnicode)
        assertEquals(1, info.segmentCount)
        assertEquals(70, info.maxSegmentLength)
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
}
