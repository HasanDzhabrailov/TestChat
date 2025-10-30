package com.example.testchat.core.data

import com.example.testchat.core.model.ChatMessage
import com.example.testchat.core.model.ChatSummary
import com.example.testchat.core.model.ChatType
import com.example.testchat.core.model.MediaMetadata
import com.example.testchat.core.model.MessageContent
import com.example.testchat.core.model.Participant
import com.example.testchat.core.model.PermissionsSnapshot
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun observeChats(): Flow<List<ChatSummary>>
    fun observeChatSummary(chatId: String): Flow<ChatSummary?>
    fun observeMessages(chatId: String): Flow<List<ChatMessage>>
    fun observeDraft(chatId: String): Flow<String>
    fun observeParticipants(chatId: String): Flow<List<Participant>>
    fun observePermissions(chatId: String): Flow<PermissionsSnapshot>
    fun observeUserCanPost(chatId: String, userId: String): Flow<Boolean>

    suspend fun loadMessages(chatId: String)
    suspend fun sendMessage(chatId: String, authorId: String, content: MessageContent)
    suspend fun editMessage(chatId: String, messageId: String, content: MessageContent)
    suspend fun deleteMessage(chatId: String, messageId: String)
    suspend fun updateDraft(chatId: String, draft: String)

    suspend fun addParticipant(chatId: String, participant: Participant)
    suspend fun removeParticipant(chatId: String, participantId: String)

    suspend fun promote(chatId: String, participantId: String)
    suspend fun demote(chatId: String, participantId: String)
    suspend fun updateRole(chatId: String, participantId: String, role: com.example.testchat.core.model.ParticipantRole)

    suspend fun getChatType(chatId: String): ChatType

    suspend fun recordAttachment(metadata: MediaMetadata)
}
