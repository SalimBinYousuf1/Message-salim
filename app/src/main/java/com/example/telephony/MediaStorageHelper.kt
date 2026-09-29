package com.example.telephony

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

object MediaStorageHelper {

    private const val DIRECTORY_NAME = "durable_media"

    /**
     * Copies media from a temporary content URI into durable app-private internal storage.
     * This guarantees images and attachments survive process death, app restarts,
     * and external cache evictions.
     */
    suspend fun saveMediaToInternalStorage(context: Context, sourceUri: Uri): File? = withContext(Dispatchers.IO) {
        try {
            val storageDir = File(context.filesDir, DIRECTORY_NAME).apply {
                if (!exists()) mkdirs()
            }

            // Determine mime type and extension
            val mimeType = context.contentResolver.getType(sourceUri) ?: "image/jpeg"
            val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
                ?: if (mimeType.contains("audio") || sourceUri.toString().contains(".m4a")) "m4a" else "jpg"

            val targetFile = File(
                storageDir,
                "salim_media_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}.$extension"
            )

            context.contentResolver.openInputStream(sourceUri)?.use { input: InputStream ->
                FileOutputStream(targetFile).use { output: FileOutputStream ->
                    input.copyTo(output)
                }
            }

            if (targetFile.exists() && targetFile.length() > 0) {
                targetFile
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}
