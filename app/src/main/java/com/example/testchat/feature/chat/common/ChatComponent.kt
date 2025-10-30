package com.example.testchat.feature.chat.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.extensions.coroutines.componentCoroutineScope
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.example.testchat.core.data.ChatRepository
import com.example.testchat.core.media.MediaAttachmentComponent
import com.example.testchat.core.model.ChatMessage
import com.example.testchat.core.model.ChatSummary
import com.example.testchat.core.model.UserProfile
import com.example.testchat.core.mvi.MviBloc
import com.example.testchat.feature.chat.message.MessageComponent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.format.DateTimeFormatter

interface ChatComponent {
    val state: StateFlow<ChatState>
    val summary: StateFlow<ChatSummary?>
    fun onIntent(intent: ChatIntent)
}

sealed interface ChatIntent {
    data object LoadMessages : ChatIntent
    data class Send(val content: MessageComponent) : ChatIntent
    data class Edit(val messageId: String, val newContent: MessageComponent) : ChatIntent
    data class Delete(val messageId: String) : ChatIntent
    data class UpdateInput(val value: String) : ChatIntent
}

sealed interface ChatState {
    data object Loading : ChatState

    @Immutable
    data class Loaded(
        val messages: List<ChatMessage>,
        val draft: String,
        val canSend: Boolean,
        val isReadOnly: Boolean
    ) : ChatState

    @Immutable
    data class Error(val message: String) : ChatState
}

internal class ChatBloc(
    private val chatId: String,
    private val currentUser: UserProfile,
    private val repository: ChatRepository,
    scope: kotlinx.coroutines.CoroutineScope
) : MviBloc<ChatIntent, ChatState>(ChatState.Loading, scope) {

    val summary = MutableStateFlow<ChatSummary?>(null)

    init {
        this.scope.launch {
            combine(
                repository.observeMessages(chatId),
                repository.observeDraft(chatId),
                repository.observeUserCanPost(chatId, currentUser.id)
            ) { messages, draft, canSend ->
                ChatState.Loaded(
                    messages = messages,
                    draft = draft,
                    canSend = canSend,
                    isReadOnly = !canSend
                )
            }
                .onStart { emitState(ChatState.Loading) }
                .catch { emitState(ChatState.Error(it.message ?: "Unable to load messages")) }
                .collect { emitState(it) }
        }

        this.scope.launch {
            repository.observeChatSummary(chatId).collect { summary.value = it }
        }
    }

    override suspend fun handleIntent(intent: ChatIntent) {
        when (intent) {
            ChatIntent.LoadMessages -> repository.loadMessages(chatId)
            is ChatIntent.Send -> repository.sendMessage(chatId, currentUser.id, intent.content)
            is ChatIntent.Edit -> repository.editMessage(chatId, intent.messageId, intent.newContent)
            is ChatIntent.Delete -> repository.deleteMessage(chatId, intent.messageId)
            is ChatIntent.UpdateInput -> repository.updateDraft(chatId, intent.value)
        }
    }
}

open class DefaultChatComponent(
    componentContext: ComponentContext,
    private val chatId: String,
    private val repository: ChatRepository,
    private val currentUser: UserProfile
) : ChatComponent, ComponentContext by componentContext {

    protected val bloc = ChatBloc(chatId, currentUser, repository, componentCoroutineScope())

    override val state: StateFlow<ChatState> = bloc.state
    override val summary: StateFlow<ChatSummary?> = bloc.summary

    init {
        bloc.dispatch(ChatIntent.LoadMessages)
        lifecycle.doOnDestroy { bloc.clear() }
    }

    override fun onIntent(intent: ChatIntent) {
        bloc.dispatch(intent)
    }
}

@Composable
fun ChatContent(
    component: ChatComponent,
    mediaAttachmentComponent: MediaAttachmentComponent,
    modifier: Modifier = Modifier
) {
    val state by component.state.collectAsState()
    val summary by component.summary.collectAsState()
    ChatContent(
        state = state,
        title = summary?.title ?: "Chat",
        onIntent = component::onIntent,
        mediaAttachmentComponent = mediaAttachmentComponent,
        modifier = modifier
    )
}

@Composable
fun ChatContent(
    state: ChatState,
    title: String,
    onIntent: (ChatIntent) -> Unit,
    mediaAttachmentComponent: MediaAttachmentComponent,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(title = { Text(title) })
        when (state) {
            ChatState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            is ChatState.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(text = "Something went wrong", style = MaterialTheme.typography.titleMedium)
                    Text(text = state.message, style = MaterialTheme.typography.bodyMedium)
                    TextButton(onClick = { onIntent(ChatIntent.LoadMessages) }) {
                        Text("Retry")
                    }
                }
            }

            is ChatState.Loaded -> {
                MessagesList(
                    messages = state.messages,
                    mediaAttachmentComponent = mediaAttachmentComponent,
                    modifier = Modifier.weight(1f)
                )
                Composer(
                    draft = state.draft,
                    enabled = state.canSend,
                    onDraftChanged = { onIntent(ChatIntent.UpdateInput(it)) },
                    onSend = {
                        if (it.isNotBlank()) {
                            onIntent(ChatIntent.Send(MessageComponent.TextMessage(it)))
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun MessagesList(
    messages: List<ChatMessage>,
    mediaAttachmentComponent: MediaAttachmentComponent,
    modifier: Modifier = Modifier
) {
    val formatter = rememberFormatter()
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(messages) { message ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = if (message.isMine) Arrangement.End else Arrangement.Start
            ) {
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (message.isMine) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.secondaryContainer
                        )
                        .padding(12.dp)
                        .widthIn(max = 320.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(text = message.author.displayName, style = MaterialTheme.typography.labelMedium)
                    message.component.Render(mediaAttachmentComponent = mediaAttachmentComponent)
                    Text(
                        text = formatter.format(message.sentAt.atZone(ZoneId.systemDefault())),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

@Composable
private fun rememberFormatter(): DateTimeFormatter =
    remember { DateTimeFormatter.ofPattern("HH:mm") }

@Composable
private fun Composer(
    draft: String,
    enabled: Boolean,
    onDraftChanged: (String) -> Unit,
    onSend: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = draft,
            onValueChange = onDraftChanged,
            enabled = enabled,
            modifier = Modifier.weight(1f),
            placeholder = { Text("Message") }
        )
        Button(onClick = { onSend(draft) }, enabled = enabled && draft.isNotBlank()) {
            Text("Send")
        }
    }
}
