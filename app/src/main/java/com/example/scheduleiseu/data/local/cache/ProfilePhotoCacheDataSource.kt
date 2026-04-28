package com.example.scheduleiseu.data.local.cache

import android.content.Context
import com.example.scheduleiseu.domain.core.model.UserPhoto
import com.example.scheduleiseu.domain.core.model.UserRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

class ProfilePhotoCacheDataSource(
    context: Context
) {
    private val cacheDir = File(
        context.applicationContext.filesDir,
        CACHE_DIR_NAME
    )

    suspend fun read(role: UserRole): UserPhoto? = withContext(Dispatchers.IO) {
        val photoFile = photoFile(role)

        if (!photoFile.exists()) {
            return@withContext null
        }

        val bytes = runCatching { photoFile.readBytes() }
            .getOrNull()
            ?.takeIf { it.isNotEmpty() }
            ?: return@withContext null

        val metadata = readMetadata(role)

        UserPhoto(
            bytes = bytes,
            sourceUrl = metadata?.optStringOrNull(KEY_SOURCE_URL),
            mimeType = metadata?.optStringOrNull(KEY_MIME_TYPE)
        )
    }

    suspend fun write(role: UserRole, photo: UserPhoto) = withContext(Dispatchers.IO) {
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }

        photoFile(role).writeBytes(photo.bytes)

        metadataFile(role).writeText(
            JSONObject()
                .putOpt(KEY_SOURCE_URL, photo.sourceUrl)
                .putOpt(KEY_MIME_TYPE, photo.mimeType)
                .toString()
        )
    }

    suspend fun clear(role: UserRole) = withContext(Dispatchers.IO) {
        photoFile(role).delete()
        metadataFile(role).delete()
    }

    private fun photoFile(role: UserRole): File {
        return File(cacheDir, "${role.name.lowercase()}_photo.bin")
    }

    private fun metadataFile(role: UserRole): File {
        return File(cacheDir, "${role.name.lowercase()}_photo.json")
    }

    private fun readMetadata(role: UserRole): JSONObject? {
        val file = metadataFile(role)

        if (!file.exists()) {
            return null
        }

        return runCatching {
            JSONObject(file.readText())
        }.getOrNull()
    }

    private fun JSONObject.optStringOrNull(name: String): String? {
        if (!has(name) || isNull(name)) {
            return null
        }

        return optString(name).takeIf { it.isNotBlank() }
    }

    private companion object {
        const val CACHE_DIR_NAME = "profile_photo_cache"
        const val KEY_SOURCE_URL = "sourceUrl"
        const val KEY_MIME_TYPE = "mimeType"
    }
}
