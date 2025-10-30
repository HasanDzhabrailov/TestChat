package com.example.testchat.core.media

import android.content.Context
import coil.request.ImageRequest
import com.example.testchat.core.model.MediaMetadata
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

interface MediaAttachmentComponent {
    fun getPreview(uri: String): ImageRequest
    suspend fun getMetadata(uri: String): MediaMetadata
    fun encodeAttachment(metadata: MediaMetadata): String
    fun decodeAttachment(serialized: String): MediaMetadata
}

class DefaultMediaAttachmentComponent(
    private val context: Context,
    private val json: Json
) : MediaAttachmentComponent {

    override fun getPreview(uri: String): ImageRequest =
        ImageRequest.Builder(context)
            .data(uri)
            .crossfade(true)
            .build()

    override suspend fun getMetadata(uri: String): MediaMetadata {
        // In a real implementation we would inspect the content resolver. Here we provide stub metadata.
        return MediaMetadata(
            uri = uri,
            mimeType = "image/*",
            sizeBytes = 512 * 1024L,
            width = 1024,
            height = 768
        )
    }

    override fun encodeAttachment(metadata: MediaMetadata): String = json.encodeToString(metadata)

    override fun decodeAttachment(serialized: String): MediaMetadata = json.decodeFromString(serialized)
}
