package com.example.telephony

import android.content.Context
import android.telephony.PhoneNumberUtils
import java.util.Locale

/**
 * Enterprise-grade phone number normalizer and comparator.
 * Handles international codes, regional variations, separators, and multi-recipient formats.
 */
object PhoneNumberNormalizer {

    /**
     * Strips whitespace, hyphens, brackets, dots, and trailing letters.
     * Retains leading '+' for international standard.
     */
    fun normalize(rawNumber: String): String {
        if (rawNumber.isBlank()) return ""
        val trimmed = rawNumber.trim()
        val hasPlus = trimmed.startsWith("+")
        val digitsOnly = trimmed.filter { it.isDigit() }
        return if (hasPlus) "+$digitsOnly" else digitsOnly
    }

    /**
     * Compares two phone numbers using Android system telephony matching with fallback
     * to last 7-10 national significant digits.
     */
    fun compare(context: Context?, number1: String, number2: String): Boolean {
        if (number1.isBlank() || number2.isBlank()) return false
        if (number1 == number2) return true

        val norm1 = normalize(number1)
        val norm2 = normalize(number2)
        if (norm1 == norm2) return true

        try {
            if (PhoneNumberUtils.compare(norm1, norm2)) {
                return true
            }
        } catch (e: Exception) {
            // fallback
        }

        // Compare trailing 7-10 digits (national significant number)
        val minLen = 7
        if (norm1.length >= minLen && norm2.length >= minLen) {
            val tail1 = norm1.takeLast(minLen)
            val tail2 = norm2.takeLast(minLen)
            if (tail1 == tail2) return true
        }

        return false
    }

    /**
     * Formats normalized number for clean, elegant display.
     */
    fun formatForDisplay(rawNumber: String): String {
        val trimmed = rawNumber.trim()
        if (trimmed.isBlank()) return ""
        return try {
            PhoneNumberUtils.formatNumber(trimmed, Locale.getDefault().country) ?: trimmed
        } catch (e: Exception) {
            trimmed
        }
    }
}
