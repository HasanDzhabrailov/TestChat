package com.example.testchat.feature.chat.permissions

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
import com.example.testchat.core.model.ParticipantRole
import com.example.testchat.core.model.PermissionsSnapshot
import com.example.testchat.core.mvi.MviBloc
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

interface PermissionsComponent {
    val state: StateFlow<PermissionsState>
    fun onIntent(intent: PermissionsIntent)
}

sealed interface PermissionsIntent {
    data object LoadPermissions : PermissionsIntent
    data class Promote(val participantId: String) : PermissionsIntent
    data class Demote(val participantId: String) : PermissionsIntent
    data class UpdateRole(val participantId: String, val role: ParticipantRole) : PermissionsIntent
}

@Immutable
data class PermissionsState(
    val roles: Map<String, ParticipantRole> = emptyMap(),
    val adminIds: Set<String> = emptySet(),
    val restrictions: Set<String> = emptySet(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

internal class PermissionsBloc(
    private val chatId: String,
    private val repository: ChatRepository,
    scope: kotlinx.coroutines.CoroutineScope
) : MviBloc<PermissionsIntent, PermissionsState>(PermissionsState(isLoading = true), scope) {

    init {
        this.scope.launch {
            repository.observePermissions(chatId)
                .onStart { emitState(PermissionsState(isLoading = true)) }
                .catch { emitState(PermissionsState(errorMessage = it.message)) }
                .collect { snapshot ->
                    emitState(
                        PermissionsState(
                            roles = snapshot.roles,
                            adminIds = snapshot.adminIds,
                            restrictions = snapshot.restrictions
                        )
                    )
                }
        }
    }

    override suspend fun handleIntent(intent: PermissionsIntent) {
        when (intent) {
            PermissionsIntent.LoadPermissions -> emitState(state.value.copy(isLoading = true))
            is PermissionsIntent.Promote -> repository.promote(chatId, intent.participantId)
            is PermissionsIntent.Demote -> repository.demote(chatId, intent.participantId)
            is PermissionsIntent.UpdateRole -> repository.updateRole(chatId, intent.participantId, intent.role)
        }
    }
}

class DefaultPermissionsComponent(
    componentContext: ComponentContext,
    chatId: String,
    repository: ChatRepository
) : PermissionsComponent, ComponentContext by componentContext {

    private val bloc = PermissionsBloc(chatId, repository, componentCoroutineScope())
    override val state: StateFlow<PermissionsState> = bloc.state

    init {
        bloc.dispatch(PermissionsIntent.LoadPermissions)
        lifecycle.doOnDestroy { bloc.clear() }
    }

    override fun onIntent(intent: PermissionsIntent) {
        bloc.dispatch(intent)
    }
}

@Composable
fun PermissionsContent(component: PermissionsComponent, modifier: Modifier = Modifier) {
    val state by component.state.collectAsState()
    PermissionsContent(state = state, onIntent = component::onIntent, modifier = modifier)
}

@Composable
fun PermissionsContent(state: PermissionsState, onIntent: (PermissionsIntent) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = "Permissions", style = MaterialTheme.typography.titleMedium)
        when {
            state.isLoading -> Text("Loading permissions…", style = MaterialTheme.typography.bodyMedium)
            state.errorMessage != null -> Text("Error: ${state.errorMessage}", color = MaterialTheme.colorScheme.error)
            else -> {
                Text(text = "Restrictions: ${state.restrictions.joinToString()}", style = MaterialTheme.typography.bodySmall)
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    items(state.roles.entries.toList()) { (participantId, role) ->
                        Column(Modifier.fillMaxWidth()) {
                            Text(text = "$participantId → ${role.name}", style = MaterialTheme.typography.bodyLarge)
                            RowActions(participantId = participantId, role = role, onIntent = onIntent)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RowActions(participantId: String, role: ParticipantRole, onIntent: (PermissionsIntent) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        TextButton(onClick = { onIntent(PermissionsIntent.Promote(participantId)) }) { Text("Promote") }
        TextButton(onClick = { onIntent(PermissionsIntent.Demote(participantId)) }) { Text("Demote") }
        if (role != ParticipantRole.OWNER) {
            TextButton(onClick = { onIntent(PermissionsIntent.UpdateRole(participantId, ParticipantRole.MEMBER)) }) {
                Text("Set as member")
            }
        }
    }
}
