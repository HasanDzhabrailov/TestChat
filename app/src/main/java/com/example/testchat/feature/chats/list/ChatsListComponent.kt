package com.example.testchat.feature.chats.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.extensions.coroutines.componentCoroutineScope
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.example.testchat.core.data.ChatRepository
import com.example.testchat.core.mvi.MviBloc
import com.example.testchat.core.model.ChatSummary
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

interface ChatsListComponent {
    val state: StateFlow<ChatsListState>
    fun onIntent(intent: ChatsListIntent)
}

sealed interface ChatsListIntent {
    data object LoadChats : ChatsListIntent
    data class OpenChat(val chatId: String) : ChatsListIntent
}

sealed interface ChatsListState {
    @Stable
    data object Loading : ChatsListState

    @Immutable
    data class Loaded(val chats: List<ChatSummary>) : ChatsListState

    @Immutable
    data class Error(val message: String) : ChatsListState
}

internal class ChatsListBloc(
    private val repository: ChatRepository,
    scope: kotlinx.coroutines.CoroutineScope
) : MviBloc<ChatsListIntent, ChatsListState>(ChatsListState.Loading, scope) {

    init {
        this.scope.launch {
            repository.observeChats()
                .onStart { emitState(ChatsListState.Loading) }
                .catch { emitState(ChatsListState.Error(it.message ?: "Unable to load chats")) }
                .collect { chats -> emitState(ChatsListState.Loaded(chats)) }
        }
    }

    override suspend fun handleIntent(intent: ChatsListIntent) {
        if (intent is ChatsListIntent.LoadChats) {
            emitState(ChatsListState.Loading)
        }
    }
}

class DefaultChatsListComponent(
    componentContext: ComponentContext,
    repository: ChatRepository,
    private val navigateToChat: (String) -> Unit,
    private val navigateToSettings: () -> Unit
) : ChatsListComponent, ComponentContext by componentContext {

    private val bloc = ChatsListBloc(repository, componentCoroutineScope())

    override val state: StateFlow<ChatsListState> = bloc.state

    init {
        bloc.dispatch(ChatsListIntent.LoadChats)
        lifecycle.doOnDestroy { bloc.clear() }
    }

    override fun onIntent(intent: ChatsListIntent) {
        when (intent) {
            ChatsListIntent.LoadChats -> bloc.dispatch(intent)
            is ChatsListIntent.OpenChat -> navigateToChat(intent.chatId)
        }
    }

    fun onSettings() = navigateToSettings()
}

@Composable
fun ChatsListContent(
    component: DefaultChatsListComponent,
    modifier: Modifier = Modifier
) {
    val state by component.state.collectAsState()
    ChatsListContent(
        state = state,
        onIntent = component::onIntent,
        onOpenSettings = component::onSettings,
        modifier = modifier
    )
}

@Composable
fun ChatsListContent(
    state: ChatsListState,
    onIntent: (ChatsListIntent) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Messages") },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(imageVector = Icons.Outlined.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { padding ->
        when (state) {
            ChatsListState.Loading -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.padding(24.dp))
                }
            }

            is ChatsListState.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(text = "Unable to load chats", style = MaterialTheme.typography.titleMedium)
                    Text(text = state.message, style = MaterialTheme.typography.bodyMedium)
                }
            }

            is ChatsListState.Loaded -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    verticalArrangement = Arrangement.Top
                ) {
                    items(state.chats) { chat ->
                        ChatRow(chat = chat, onClick = { onIntent(ChatsListIntent.OpenChat(chat.id)) })
                        Divider()
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatRow(chat: ChatSummary, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = chat.title, style = MaterialTheme.typography.titleMedium)
            if (chat.unreadCount > 0) {
                Text(text = chat.unreadCount.toString(), style = MaterialTheme.typography.labelMedium)
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = chat.lastMessagePreview,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
