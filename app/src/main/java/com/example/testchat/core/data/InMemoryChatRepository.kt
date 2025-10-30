package com.example.testchat.core.data

import com.example.testchat.core.model.ChatMessage
import com.example.testchat.core.model.ChatSummary
import com.example.testchat.core.model.ChatType
import com.example.testchat.core.model.MediaMetadata
import com.example.testchat.core.model.MessageContent
import com.example.testchat.core.model.Participant
import com.example.testchat.core.model.ParticipantRole
import com.example.testchat.core.model.PermissionsSnapshot
import com.example.testchat.core.model.UserProfile
import com.example.testchat.feature.chat.message.MessageComponent
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.time.Instant
import java.util.UUID

class InMemoryChatRepository(
    private val currentUser: UserProfile
) : ChatRepository {

    private val chats = MutableStateFlow(
        listOf(
            ChatSummary(
                id = "chat-direct",
                title = "Alex Johnson",
                lastMessagePreview = "Let's catch up soon!",
                unreadCount = 2,
                type = ChatType.DIRECT
            ),
            ChatSummary(
                id = "chat-group",
                title = "Product Squad",
                lastMessagePreview = "Standup in 5 minutes",
                unreadCount = 0,
                type = ChatType.GROUP
            ),
            ChatSummary(
                id = "chat-channel",
                title = "Release Notes",
                lastMessagePreview = "Version 2.3 rolling out",
                unreadCount = 0,
                type = ChatType.CHANNEL
            )
        )
    )

    private val drafts = MutableStateFlow<Map<String, String>>(emptyMap())

    private val participants = MutableStateFlow(
        mapOf(
            "chat-direct" to listOf(
                Participant(currentUser.id, currentUser.displayName, currentUser.avatarUrl, ParticipantRole.MEMBER),
                Participant("alex", "Alex Johnson", null, ParticipantRole.MEMBER)
            ),
            "chat-group" to listOf(
                Participant(currentUser.id, currentUser.displayName, currentUser.avatarUrl, ParticipantRole.OWNER),
                Participant("sam", "Samantha Lee", null, ParticipantRole.ADMIN),
                Participant("tariq", "Tariq Ali", null, ParticipantRole.MODERATOR),
                Participant("maya", "Maya N.", null, ParticipantRole.MEMBER)
            ),
            "chat-channel" to listOf(
                Participant(currentUser.id, currentUser.displayName, currentUser.avatarUrl, ParticipantRole.ADMIN),
                Participant("devops", "DevOps Bot", null, ParticipantRole.MODERATOR),
                Participant("viewer", "Read Only", null, ParticipantRole.READER)
            )
        )
    )

    private val permissions = MutableStateFlow(
        mapOf(
            "chat-direct" to PermissionsSnapshot(
                roles = mapOf(
                    currentUser.id to ParticipantRole.MEMBER,
                    "alex" to ParticipantRole.MEMBER
                ),
                adminIds = emptySet(),
                restrictions = emptySet()
            ),
            "chat-group" to PermissionsSnapshot(
                roles = mapOf(
                    currentUser.id to ParticipantRole.OWNER,
                    "sam" to ParticipantRole.ADMIN,
                    "tariq" to ParticipantRole.MODERATOR,
                    "maya" to ParticipantRole.MEMBER
                ),
                adminIds = setOf(currentUser.id, "sam", "tariq"),
                restrictions = emptySet()
            ),
            "chat-channel" to PermissionsSnapshot(
                roles = mapOf(
                    currentUser.id to ParticipantRole.ADMIN,
                    "devops" to ParticipantRole.MODERATOR,
                    "viewer" to ParticipantRole.READER
                ),
                adminIds = setOf(currentUser.id, "devops"),
                restrictions = setOf("POST_BLOCKED")
            )
        )
    )

    private val messages = MutableStateFlow(
        mapOf(
            "chat-direct" to listOf(
                message(
                    chatId = "chat-direct",
                    authorId = "alex",
                    content = MessageComponent.TextMessage("Are we still on for tomorrow?"),
                    isMine = false
                ),
                message(
                    chatId = "chat-direct",
                    authorId = currentUser.id,
                    content = MessageComponent.TextMessage("Absolutely, see you at 9."),
                    isMine = true
                )
            ),
            "chat-group" to listOf(
                message(
                    chatId = "chat-group",
                    authorId = "sam",
                    content = MessageComponent.InvitationMessage("Product Squad", "Sprint planning is today."),
                    isMine = false
                ),
                message(
                    chatId = "chat-group",
                    authorId = currentUser.id,
                    content = MessageComponent.TextMessage("Reminder: please update your tickets."),
                    isMine = true
                ),
                message(
                    chatId = "chat-group",
                    authorId = "maya",
                    content = MessageComponent.PhotoMessage(
                        caption = "Design draft",
                        media = MediaMetadata(
                            uri = "https://picsum.photos/seed/design/400/300",
                            mimeType = "image/jpeg",
                            sizeBytes = 400_000,
                            width = 400,
                            height = 300
                        )
                    ),
                    isMine = false
                )
            ),
            "chat-channel" to listOf(
                message(
                    chatId = "chat-channel",
                    authorId = "devops",
                    content = MessageComponent.LinkMessage(
                        url = "https://example.com/releases/2.3",
                        title = "Release 2.3",
                        description = "Highlights of this week's release"
                    ),
                    isMine = false
                )
            )
        )
    )

    private val attachments = MutableStateFlow<List<MediaMetadata>>(emptyList())

    override fun observeChats(): Flow<List<ChatSummary>> = chats.asStateFlow()

    override fun observeChatSummary(chatId: String): Flow<ChatSummary?> =
        chats.map { list -> list.find { it.id == chatId } }

    override fun observeMessages(chatId: String): Flow<List<ChatMessage>> =
        messages.map { it[chatId] ?: emptyList() }

    override fun observeDraft(chatId: String): Flow<String> =
        drafts.map { it[chatId] ?: "" }

    override fun observeParticipants(chatId: String): Flow<List<Participant>> =
        participants.map { it[chatId] ?: emptyList() }

    override fun observePermissions(chatId: String): Flow<PermissionsSnapshot> =
        permissions.map { it[chatId] ?: PermissionsSnapshot(emptyMap(), emptySet(), emptySet()) }

    override fun observeUserCanPost(chatId: String, userId: String): Flow<Boolean> =
        permissions.map { perms -> (perms[chatId] ?: PermissionsSnapshot(emptyMap(), emptySet(), emptySet())).canPost(userId) }

    override suspend fun loadMessages(chatId: String) {
        delay(300)
    }

    override suspend fun sendMessage(chatId: String, authorId: String, content: MessageContent) {
        val author = participants.value[chatId]?.firstOrNull { it.id == authorId }
            ?: Participant(authorId, currentUser.displayName, currentUser.avatarUrl, ParticipantRole.MEMBER)
        messages.update { map ->
            val updated = map[chatId].orEmpty() + message(
                chatId = chatId,
                authorId = authorId,
                content = content,
                isMine = authorId == currentUser.id,
                author = author
            )
            map + (chatId to updated)
        }
        updateChatPreview(chatId, content)
        drafts.update { it + (chatId to "") }
    }

    override suspend fun editMessage(chatId: String, messageId: String, content: MessageContent) {
        messages.update { map ->
            val updated = map[chatId].orEmpty().map { existing ->
                if (existing.id == messageId) existing.copy(component = content) else existing
            }
            map + (chatId to updated)
        }
        updateChatPreview(chatId, content)
    }

    override suspend fun deleteMessage(chatId: String, messageId: String) {
        messages.update { map ->
            val updated = map[chatId].orEmpty().filterNot { it.id == messageId }
            map + (chatId to updated)
        }
        val lastContent = messages.value[chatId]?.lastOrNull()?.component
        if (lastContent != null) {
            updateChatPreview(chatId, lastContent)
        }
    }

    override suspend fun updateDraft(chatId: String, draft: String) {
        drafts.update { it + (chatId to draft) }
    }

    override suspend fun addParticipant(chatId: String, participant: Participant) {
        participants.update { map ->
            val updated = map[chatId].orEmpty().filterNot { it.id == participant.id } + participant
            map + (chatId to updated)
        }
        permissions.update { map ->
            val snapshot = map[chatId]
            if (snapshot != null) {
                map + (chatId to snapshot.copy(roles = snapshot.roles + (participant.id to participant.role)))
            } else {
                map
            }
        }
    }

    override suspend fun removeParticipant(chatId: String, participantId: String) {
        participants.update { map ->
            val updated = map[chatId].orEmpty().filterNot { it.id == participantId }
            map + (chatId to updated)
        }
        permissions.update { map ->
            val snapshot = map[chatId]
            if (snapshot != null) {
                map + (chatId to snapshot.copy(roles = snapshot.roles - participantId, adminIds = snapshot.adminIds - participantId))
            } else map
        }
    }

    override suspend fun promote(chatId: String, participantId: String) {
        permissions.update { map ->
            val snapshot = map[chatId]
            if (snapshot != null) {
                val newRole = when (snapshot.roles[participantId]) {
                    ParticipantRole.MEMBER -> ParticipantRole.MODERATOR
                    ParticipantRole.MODERATOR -> ParticipantRole.ADMIN
                    ParticipantRole.READER -> ParticipantRole.MEMBER
                    ParticipantRole.ADMIN, ParticipantRole.OWNER, null -> ParticipantRole.ADMIN
                }
                map + (chatId to snapshot.copy(
                    roles = snapshot.roles + (participantId to newRole),
                    adminIds = snapshot.adminIds + participantId
                ))
            } else map
        }
    }

    override suspend fun demote(chatId: String, participantId: String) {
        permissions.update { map ->
            val snapshot = map[chatId]
            if (snapshot != null) {
                val newRole = when (snapshot.roles[participantId]) {
                    ParticipantRole.ADMIN -> ParticipantRole.MODERATOR
                    ParticipantRole.MODERATOR -> ParticipantRole.MEMBER
                    ParticipantRole.MEMBER -> ParticipantRole.READER
                    else -> ParticipantRole.READER
                }
                map + (chatId to snapshot.copy(
                    roles = snapshot.roles + (participantId to newRole),
                    adminIds = snapshot.adminIds - participantId
                ))
            } else map
        }
    }

    override suspend fun updateRole(chatId: String, participantId: String, role: ParticipantRole) {
        permissions.update { map ->
            val snapshot = map[chatId]
            if (snapshot != null) {
                val isAdminRole = role == ParticipantRole.ADMIN || role == ParticipantRole.OWNER || role == ParticipantRole.MODERATOR
                val admins = if (isAdminRole) snapshot.adminIds + participantId else snapshot.adminIds - participantId
                map + (chatId to snapshot.copy(roles = snapshot.roles + (participantId to role), adminIds = admins))
            } else map
        }
    }

    override suspend fun getChatType(chatId: String): ChatType {
        return chats.value.first { it.id == chatId }.type
    }

    override suspend fun recordAttachment(metadata: MediaMetadata) {
        attachments.update { it + metadata }
    }

    private fun updateChatPreview(chatId: String, content: MessageContent) {
        val preview = when (content) {
            is MessageComponent.TextMessage -> content.text
            is MessageComponent.PhotoMessage -> "Photo"
            is MessageComponent.VideoMessage -> "Video"
            is MessageComponent.FileMessage -> content.fileName
            is MessageComponent.VoiceMessage -> "Voice message"
            is MessageComponent.LinkMessage -> content.title ?: content.url
            is MessageComponent.ContactShareMessage -> "Shared contact"
            is MessageComponent.InvitationMessage -> "Invitation"
        }
        chats.update { list ->
            list.map { summary ->
                if (summary.id == chatId) summary.copy(lastMessagePreview = preview) else summary
            }
        }
    }

    private fun message(
        chatId: String,
        authorId: String,
        content: MessageContent,
        isMine: Boolean,
        author: Participant? = null
    ): ChatMessage {
        val participant = author ?: participants.value[chatId]?.first { it.id == authorId }
        return ChatMessage(
            id = UUID.randomUUID().toString(),
            chatId = chatId,
            author = participant ?: Participant(authorId, authorId.replaceFirstChar { it.titlecase() }, null, ParticipantRole.MEMBER),
            sentAt = Instant.now(),
            isMine = isMine,
            component = content
        )
    }
}
