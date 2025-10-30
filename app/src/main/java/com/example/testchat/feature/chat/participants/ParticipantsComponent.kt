package com.example.testchat.feature.chat.participants

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.extensions.coroutines.componentCoroutineScope
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.example.testchat.core.data.ChatRepository
import com.example.testchat.core.model.Participant
import com.example.testchat.core.mvi.MviBloc
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

interface ParticipantsComponent {
    val state: StateFlow<ParticipantsState>
    fun onIntent(intent: ParticipantsIntent)
}

sealed interface ParticipantsIntent {
    data object LoadParticipants : ParticipantsIntent
    data class AddUser(val participant: Participant) : ParticipantsIntent
    data class RemoveUser(val participantId: String) : ParticipantsIntent
}

@Immutable
data class ParticipantsState(
    val participants: List<Participant> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

internal class ParticipantsBloc(
    private val chatId: String,
    private val repository: ChatRepository,
    scope: kotlinx.coroutines.CoroutineScope
) : MviBloc<ParticipantsIntent, ParticipantsState>(ParticipantsState(isLoading = true), scope) {

    init {
        this.scope.launch {
            repository.observeParticipants(chatId)
                .onStart { emitState(ParticipantsState(isLoading = true)) }
                .catch { emitState(ParticipantsState(errorMessage = it.message)) }
                .collect { emitState(ParticipantsState(participants = it)) }
        }
    }

    override suspend fun handleIntent(intent: ParticipantsIntent) {
        when (intent) {
            ParticipantsIntent.LoadParticipants -> emitState(currentState().copy(isLoading = true))
            is ParticipantsIntent.AddUser -> repository.addParticipant(chatId, intent.participant)
            is ParticipantsIntent.RemoveUser -> repository.removeParticipant(chatId, intent.participantId)
        }
    }

    private fun currentState(): ParticipantsState = state.value
}

class DefaultParticipantsComponent(
    componentContext: ComponentContext,
    chatId: String,
    repository: ChatRepository
) : ParticipantsComponent, ComponentContext by componentContext {

    private val bloc = ParticipantsBloc(chatId, repository, componentCoroutineScope())
    override val state: StateFlow<ParticipantsState> = bloc.state

    init {
        bloc.dispatch(ParticipantsIntent.LoadParticipants)
        lifecycle.doOnDestroy { bloc.clear() }
    }

    override fun onIntent(intent: ParticipantsIntent) {
        bloc.dispatch(intent)
    }
}

@Composable
fun ParticipantsContent(component: ParticipantsComponent, modifier: Modifier = Modifier) {
    val state by component.state.collectAsState()
    ParticipantsContent(state = state, onIntent = component::onIntent, modifier = modifier)
}

@Composable
fun ParticipantsContent(state: ParticipantsState, onIntent: (ParticipantsIntent) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = "Participants", style = MaterialTheme.typography.titleMedium)
        when {
            state.isLoading -> Text("Loading…", style = MaterialTheme.typography.bodyMedium)
            state.errorMessage != null -> Text("Error: ${state.errorMessage}", color = MaterialTheme.colorScheme.error)
            else -> {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    items(state.participants) { participant ->
                        Column(Modifier.fillMaxWidth()) {
                            Text(text = participant.displayName, style = MaterialTheme.typography.bodyLarge)
                            Text(text = participant.role.name, style = MaterialTheme.typography.labelMedium)
                            TextButton(onClick = { onIntent(ParticipantsIntent.RemoveUser(participant.id)) }) {
                                Text("Remove")
                            }
                        }
                    }
                }
            }
        }
    }
}
