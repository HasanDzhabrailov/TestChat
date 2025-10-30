package com.example.testchat.feature.chat.message

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.InsertLink
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.testchat.core.media.MediaAttachmentComponent
import com.example.testchat.core.model.MediaMetadata
import com.example.testchat.core.model.MessageContent

sealed class MessageComponent : MessageContent {

    @Composable
    abstract fun Render(
        mediaAttachmentComponent: MediaAttachmentComponent,
        modifier: Modifier = Modifier
    )

    @Immutable
    data class TextMessage(val text: String) : MessageComponent() {
        @Composable
        override fun Render(mediaAttachmentComponent: MediaAttachmentComponent, modifier: Modifier) {
            Text(text = text, style = MaterialTheme.typography.bodyMedium, modifier = modifier)
        }
    }

    @Immutable
    data class PhotoMessage(val caption: String?, val media: MediaMetadata) : MessageComponent() {
        @Composable
        override fun Render(mediaAttachmentComponent: MediaAttachmentComponent, modifier: Modifier) {
            Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AsyncImage(
                    model = mediaAttachmentComponent.getPreview(media.uri),
                    contentDescription = caption,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.medium)
                        .background(Color.Black)
                        .size(width = 220.dp, height = 180.dp)
                )
                caption?.let {
                    Text(text = it, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }

    @Immutable
    data class VideoMessage(val caption: String?, val media: MediaMetadata) : MessageComponent() {
        @Composable
        override fun Render(mediaAttachmentComponent: MediaAttachmentComponent, modifier: Modifier) {
            Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.medium)
                        .background(Color.Black)
                        .size(width = 240.dp, height = 180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = mediaAttachmentComponent.getPreview(media.uri),
                        contentDescription = caption,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize()
                    )
                    Icon(
                        imageVector = Icons.Outlined.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                            .padding(8.dp),
                        tint = Color.White
                    )
                }
                caption?.let {
                    Text(text = it, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }

    @Immutable
    data class FileMessage(val fileName: String, val media: MediaMetadata) : MessageComponent() {
        @Composable
        override fun Render(mediaAttachmentComponent: MediaAttachmentComponent, modifier: Modifier) {
            Row(
                modifier = modifier
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(imageVector = Icons.Outlined.UploadFile, contentDescription = null)
                Column(Modifier.weight(1f)) {
                    Text(text = fileName, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = "${media.mimeType} • ${media.sizeBytes / 1024} KB",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    @Immutable
    data class VoiceMessage(val media: MediaMetadata) : MessageComponent() {
        @Composable
        override fun Render(mediaAttachmentComponent: MediaAttachmentComponent, modifier: Modifier) {
            Row(
                modifier = modifier
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(imageVector = Icons.Outlined.PlayArrow, contentDescription = null)
                Column(Modifier.weight(1f)) {
                    Text(text = "Voice message", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = media.durationSeconds?.let { "${it}s" } ?: "Unknown duration",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }

    @Immutable
    data class LinkMessage(val url: String, val title: String?, val description: String?) : MessageComponent() {
        @Composable
        override fun Render(mediaAttachmentComponent: MediaAttachmentComponent, modifier: Modifier) {
            Column(
                modifier = modifier
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Outlined.InsertLink, contentDescription = null)
                    Text(
                        text = url,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .clickable { }
                    )
                }
                title?.let {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = it, style = MaterialTheme.typography.titleMedium)
                }
                description?.let {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }

    @Immutable
    data class ContactShareMessage(val name: String, val phone: String) : MessageComponent() {
        @Composable
        override fun Render(mediaAttachmentComponent: MediaAttachmentComponent, modifier: Modifier) {
            Row(
                modifier = modifier
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(imageVector = Icons.Outlined.Person, contentDescription = null)
                Column(Modifier.weight(1f)) {
                    Text(text = name, style = MaterialTheme.typography.titleSmall)
                    Text(text = phone, style = MaterialTheme.typography.bodySmall)
                }
                Icon(imageVector = Icons.Outlined.Phone, contentDescription = null)
            }
        }
    }

    @Immutable
    data class InvitationMessage(val groupName: String, val description: String?) : MessageComponent() {
        @Composable
        override fun Render(mediaAttachmentComponent: MediaAttachmentComponent, modifier: Modifier) {
            Column(
                modifier = modifier
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = "Invitation", style = MaterialTheme.typography.titleMedium)
                Text(text = "Join $groupName", style = MaterialTheme.typography.titleLarge)
                description?.let {
                    Text(text = it, style = MaterialTheme.typography.bodyMedium)
                }
                AssistChip(onClick = { /* Accept invitation */ }, label = { Text("Accept") })
            }
        }
    }
}
