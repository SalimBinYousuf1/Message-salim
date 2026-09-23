package com.example.data

import android.content.Context
import com.example.SalimApplication
import com.example.data.local.BlockedContact
import com.example.data.local.ConversationMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object BackupRestoreHelper {

    suspend fun createBackupJson(context: Context): String = withContext(Dispatchers.IO) {
        val app = context.applicationContext as SalimApplication
        val db = app.database
        val metadataList = db.conversationDao().getAllMetadata().first()
        val blockedList = db.blockedContactDao().getAllBlockedFlow().first()
        val settings = app.preferencesRepository.settingsFlow.first()

        val rootObj = JSONObject()
        rootObj.put("version", 1)
        rootObj.put("timestamp", System.currentTimeMillis())

        val metaArray = JSONArray()
        for (m in metadataList) {
            val item = JSONObject().apply {
                put("threadId", m.threadId)
                put("isPinned", m.isPinned)
                put("isArchived", m.isArchived)
                put("customName", m.customName ?: "")
                put("draft", m.draft ?: "")
            }
            metaArray.put(item)
        }
        rootObj.put("metadata", metaArray)

        val blockedArray = JSONArray()
        for (b in blockedList) {
            val item = JSONObject().apply {
                put("address", b.address)
                put("displayName", b.displayName)
                put("blockedTimestamp", b.blockedTimestamp)
            }
            blockedArray.put(item)
        }
        rootObj.put("blocked", blockedArray)

        val settingsObj = JSONObject().apply {
            put("themeMode", settings.themeMode.name)
            put("pureBlackOled", settings.pureBlackOled)
            put("accentPalette", settings.accentPalette.name)
            put("ambientBackground", settings.ambientBackground.name)
            put("reducedMotion", settings.reducedMotion)
            put("reducedTransparency", settings.reducedTransparency)
            put("hapticsEnabled", settings.hapticsEnabled)
            put("biometricLockEnabled", settings.biometricLockEnabled)
            put("flagSecureEnabled", settings.flagSecureEnabled)
        }
        rootObj.put("settings", settingsObj)

        val backupDir = File(context.filesDir, "backups").apply { mkdirs() }
        val backupFile = File(backupDir, "salim_backup_${System.currentTimeMillis()}.json")
        backupFile.writeText(rootObj.toString(2))

        rootObj.toString(2)
    }

    suspend fun restoreFromJson(context: Context, jsonStr: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val app = context.applicationContext as SalimApplication
            val db = app.database
            val rootObj = JSONObject(jsonStr)

            if (rootObj.has("metadata")) {
                val metaArray = rootObj.getJSONArray("metadata")
                for (i in 0 until metaArray.length()) {
                    val item = metaArray.getJSONObject(i)
                    val metadata = ConversationMetadata(
                        threadId = item.getLong("threadId"),
                        isPinned = item.optBoolean("isPinned", false),
                        isArchived = item.optBoolean("isArchived", false),
                        customName = item.optString("customName").takeIf { it.isNotBlank() },
                        draft = item.optString("draft").takeIf { it.isNotBlank() }
                    )
                    db.conversationDao().insertOrUpdate(metadata)
                }
            }

            if (rootObj.has("blocked")) {
                val blockedArray = rootObj.getJSONArray("blocked")
                for (i in 0 until blockedArray.length()) {
                    val item = blockedArray.getJSONObject(i)
                    val contact = BlockedContact(
                        address = item.getString("address"),
                        displayName = item.optString("displayName", item.getString("address")),
                        blockedTimestamp = item.optLong("blockedTimestamp", System.currentTimeMillis())
                    )
                    db.blockedContactDao().block(contact)
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }
}
