package com.example.testchat.core.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable
import java.time.Instant

/** Represents the type of chat surface we can navigate to. */
@Serializable
enum class ChatType {
    DIRECT,
    GROUP,
    CHANNEL
}

@Immutable
@Serializable
data class ChatSummary(
    val id: String,
    val title: String,
    val lastMessagePreview: String,
    val unreadCount: Int,
    val type: ChatType
)

@Immutable
@Serializable
data class Participant(
    val id: String,
    val displayName: String,
    val avatarUrl: String?,
    val role: ParticipantRole
)

@Serializable
enum class ParticipantRole {
    OWNER,
    ADMIN,
    MODERATOR,
    MEMBER,
    READER
}

@Immutable
@Serializable
data class ChatMessage(
    val id: String,
    val chatId: String,
    val author: Participant,
    val sentAt: Instant,
    val isMine: Boolean,
    val component: MessageContent
)

/**
 * Lightweight metadata for media attachments that can be encoded/decoded for persistence.
 */
@Immutable
@Serializable
data class MediaMetadata(
    val uri: String,
    val mimeType: String,
    val sizeBytes: Long,
    val width: Int? = null,
    val height: Int? = null,
    val durationSeconds: Int? = null
)

/**
 * The serialized form of message content, persisted via [MessageContent].
 */
@Serializable
sealed interface MessageContent
