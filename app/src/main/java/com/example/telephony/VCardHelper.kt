package com.example.telephony

import android.content.Intent
import android.provider.ContactsContract

data class VCardData(
    val name: String,
    val phone: String,
    val email: String? = null,
    val organization: String? = null
)

object VCardHelper {

    fun isVCard(text: String): Boolean {
        val trimmed = text.trim()
        return trimmed.startsWith("BEGIN:VCARD", ignoreCase = true) ||
                (trimmed.contains("BEGIN:VCARD", ignoreCase = true) && trimmed.contains("END:VCARD", ignoreCase = true))
    }

    fun parseVCard(text: String): VCardData? {
        if (!isVCard(text)) return null

        var name = ""
        var phone = ""
        var email: String? = null
        var org: String? = null

        val lines = text.lines()
        for (line in lines) {
            val trimmed = line.trim()
            when {
                trimmed.startsWith("FN:", ignoreCase = true) -> {
                    name = trimmed.substring(3).trim()
                }
                trimmed.startsWith("N:", ignoreCase = true) && name.isBlank() -> {
                    val parts = trimmed.substring(2).split(";")
                    val lastName = parts.getOrNull(0)?.trim().orEmpty()
                    val firstName = parts.getOrNull(1)?.trim().orEmpty()
                    name = "$firstName $lastName".trim()
                }
                trimmed.startsWith("TEL", ignoreCase = true) -> {
                    val colonIdx = trimmed.indexOf(':')
                    if (colonIdx != -1) {
                        phone = trimmed.substring(colonIdx + 1).trim()
                    }
                }
                trimmed.startsWith("EMAIL", ignoreCase = true) -> {
                    val colonIdx = trimmed.indexOf(':')
                    if (colonIdx != -1) {
                        email = trimmed.substring(colonIdx + 1).trim()
                    }
                }
                trimmed.startsWith("ORG:", ignoreCase = true) -> {
                    org = trimmed.substring(4).trim()
                }
            }
        }

        if (name.isBlank() && phone.isBlank()) return null
        return VCardData(
            name = if (name.isNotBlank()) name else phone,
            phone = phone,
            email = email,
            organization = org
        )
    }

    fun generateVCard(name: String, phone: String, email: String? = null, org: String? = null): String {
        val sb = StringBuilder()
        sb.appendLine("BEGIN:VCARD")
        sb.appendLine("VERSION:3.0")
        sb.appendLine("FN:$name")
        val parts = name.split(" ")
        val first = parts.firstOrNull().orEmpty()
        val last = if (parts.size > 1) parts.drop(1).joinToString(" ") else ""
        sb.appendLine("N:$last;$first;;;")
        if (phone.isNotBlank()) {
            sb.appendLine("TEL;TYPE=CELL:$phone")
        }
        if (!email.isNullOrBlank()) {
            sb.appendLine("EMAIL;TYPE=INTERNET:$email")
        }
        if (!org.isNullOrBlank()) {
            sb.appendLine("ORG:$org")
        }
        sb.appendLine("END:VCARD")
        return sb.toString().trim()
    }

    fun createInsertContactIntent(vCard: VCardData): Intent {
        return Intent(Intent.ACTION_INSERT).apply {
            type = ContactsContract.Contacts.CONTENT_TYPE
            putExtra(ContactsContract.Intents.Insert.NAME, vCard.name)
            if (vCard.phone.isNotBlank()) {
                putExtra(ContactsContract.Intents.Insert.PHONE, vCard.phone)
                putExtra(ContactsContract.Intents.Insert.PHONE_TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
            }
            if (!vCard.email.isNullOrBlank()) {
                putExtra(ContactsContract.Intents.Insert.EMAIL, vCard.email)
            }
            if (!vCard.organization.isNullOrBlank()) {
                putExtra(ContactsContract.Intents.Insert.COMPANY, vCard.organization)
            }
        }
    }
}
