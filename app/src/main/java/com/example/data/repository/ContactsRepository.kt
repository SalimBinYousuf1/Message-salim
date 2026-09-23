package com.example.data.repository

import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import com.example.data.model.ContactItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ContactsRepository(private val context: Context) {

    suspend fun getContacts(searchQuery: String = ""): List<ContactItem> = withContext(Dispatchers.IO) {
        val contacts = mutableListOf<ContactItem>()
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            return@withContext contacts
        }

        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.PHOTO_URI,
            ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI
        )

        val selection: String?
        val selectionArgs: Array<String>?
        if (searchQuery.isNotBlank()) {
            selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ? OR ${ContactsContract.CommonDataKinds.Phone.NUMBER} LIKE ?"
            val pattern = "%$searchQuery%"
            selectionArgs = arrayOf(pattern, pattern)
        } else {
            selection = null
            selectionArgs = null
        }

        val seenNumbers = mutableSetOf<String>()

        try {
            context.contentResolver.query(
                uri,
                projection,
                selection,
                selectionArgs,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                val keyIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY)
                val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val photoIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI)
                val thumbIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI)

                while (cursor.moveToNext()) {
                    val id = if (idIdx != -1) cursor.getLong(idIdx) else 0L
                    val key = if (keyIdx != -1) cursor.getString(keyIdx) ?: "" else ""
                    val name = if (nameIdx != -1) cursor.getString(nameIdx) ?: "Unknown" else "Unknown"
                    val number = if (numberIdx != -1) cursor.getString(numberIdx) ?: "" else ""
                    val photo = if (photoIdx != -1) cursor.getString(photoIdx) else null
                    val thumb = if (thumbIdx != -1) cursor.getString(thumbIdx) else null

                    val normalizedNumber = number.replace(Regex("[^0-9+]"), "")
                    if (normalizedNumber.isNotBlank() && !seenNumbers.contains(normalizedNumber)) {
                        seenNumbers.add(normalizedNumber)
                        contacts.add(
                            ContactItem(
                                id = id,
                                lookupKey = key,
                                displayName = name,
                                phoneNumber = number.trim(),
                                photoUri = photo ?: thumb
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            // ignore
        }

        contacts
    }
}
