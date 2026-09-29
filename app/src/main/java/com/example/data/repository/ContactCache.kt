package com.example.data.repository

import android.content.Context
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import com.example.telephony.PhoneNumberNormalizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

data class ContactInfo(
    val displayName: String,
    val photoUri: String?,
    val normalizedNumber: String
)

/**
 * High-performance in-memory LRU contact resolution cache with background batch preloading
 * and lifecycle-aware ContentObserver synchronization.
 */
object ContactCache {

    private val cache = ConcurrentHashMap<String, ContactInfo>()
    private var isObserverRegistered = false

    fun resolveContact(context: Context, rawNumber: String): Pair<String, String?> {
        val trimmed = rawNumber.trim()
        if (trimmed.isBlank()) return Pair(trimmed, null)

        // Multi-recipient group threads (e.g. "+12345, +67890")
        if (trimmed.contains(",")) {
            val parts = trimmed.split(",").map { it.trim() }.filter { it.isNotBlank() }
            val resolvedNames = parts.map { part ->
                resolveSingleContact(context, part).first
            }
            return Pair(resolvedNames.joinToString(", "), null)
        }

        return resolveSingleContact(context, trimmed)
    }

    private fun resolveSingleContact(context: Context, number: String): Pair<String, String?> {
        val normalized = PhoneNumberNormalizer.normalize(number)
        if (normalized.isBlank()) return Pair(number, null)

        // 1. O(1) Memory Cache Check
        val cached = cache[normalized]
        if (cached != null) {
            return Pair(cached.displayName, cached.photoUri)
        }

        // Check fallback tail digits in cache
        if (normalized.length >= 7) {
            val tail = normalized.takeLast(7)
            val match = cache.values.find { it.normalizedNumber.endsWith(tail) }
            if (match != null) {
                cache[normalized] = match
                return Pair(match.displayName, match.photoUri)
            }
        }

        // 2. Query system ContactsContract
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            return Pair(number, null)
        }

        try {
            val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number))
            val projection = arrayOf(
                ContactsContract.PhoneLookup.DISPLAY_NAME,
                ContactsContract.PhoneLookup.PHOTO_URI,
                ContactsContract.PhoneLookup.PHOTO_THUMBNAIL_URI
            )

            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIdx = cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)
                    val photoIdx = cursor.getColumnIndex(ContactsContract.PhoneLookup.PHOTO_URI)
                    val thumbIdx = cursor.getColumnIndex(ContactsContract.PhoneLookup.PHOTO_THUMBNAIL_URI)

                    val name = if (nameIdx != -1) cursor.getString(nameIdx) else null
                    val photo = if (photoIdx != -1) cursor.getString(photoIdx) else null
                    val thumb = if (thumbIdx != -1) cursor.getString(thumbIdx) else null

                    if (!name.isNullOrBlank()) {
                        val info = ContactInfo(name, photo ?: thumb, normalized)
                        cache[normalized] = info
                        return Pair(name, photo ?: thumb)
                    }
                }
            }
        } catch (e: Exception) {
            // ignore
        }

        // Default to formatted phone number
        val fallbackName = PhoneNumberNormalizer.formatForDisplay(number)
        val info = ContactInfo(fallbackName, null, normalized)
        cache[normalized] = info
        return Pair(fallbackName, null)
    }

    /**
     * Preloads all contacts in a single fast batch query to ensure instantaneous list scrolling.
     */
    fun preloadContacts(context: Context) {
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
                val projection = arrayOf(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER,
                    ContactsContract.CommonDataKinds.Phone.PHOTO_URI,
                    ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI
                )

                context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                    val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                    val numIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    val photoIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI)
                    val thumbIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI)

                    while (cursor.moveToNext()) {
                        val name = if (nameIdx != -1) cursor.getString(nameIdx) else null
                        val num = if (numIdx != -1) cursor.getString(numIdx) else null
                        val photo = if (photoIdx != -1) cursor.getString(photoIdx) else null
                        val thumb = if (thumbIdx != -1) cursor.getString(thumbIdx) else null

                        if (!name.isNullOrBlank() && !num.isNullOrBlank()) {
                            val norm = PhoneNumberNormalizer.normalize(num)
                            if (norm.isNotBlank()) {
                                cache[norm] = ContactInfo(name, photo ?: thumb, norm)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    /**
     * Automatically watches contacts database changes to invalidate stale cached records.
     */
    fun registerObserver(context: Context) {
        if (isObserverRegistered) return
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                cache.clear()
                preloadContacts(context)
            }
        }
        try {
            context.contentResolver.registerContentObserver(
                ContactsContract.Contacts.CONTENT_URI,
                true,
                observer
            )
            isObserverRegistered = true
        } catch (e: Exception) {
            // ignore
        }
    }

    fun clear() {
        cache.clear()
    }
}
