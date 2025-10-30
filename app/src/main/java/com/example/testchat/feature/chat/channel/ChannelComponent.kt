package com.example.testchat.feature.chat.channel

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.ComponentContext
import com.example.testchat.core.data.ChatRepository
import com.example.testchat.core.media.MediaAttachmentComponent
import com.example.testchat.core.model.UserProfile
import com.example.testchat.feature.chat.common.ChatComponent
import com.example.testchat.feature.chat.common.ChatContent
import com.example.testchat.feature.chat.common.ChatState
import com.example.testchat.feature.chat.common.DefaultChatComponent
import com.example.testchat.feature.chat.participants.DefaultParticipantsComponent
import com.example.testchat.feature.chat.participants.ParticipantsComponent
import com.example.testchat.feature.chat.participants.ParticipantsContent
import com.example.testchat.feature.chat.permissions.DefaultPermissionsComponent
import com.example.testchat.feature.chat.permissions.PermissionsComponent
import com.example.testchat.feature.chat.permissions.PermissionsContent

interface ChannelComponent : ChatComponent {
    val participantsComponent: ParticipantsComponent
    val permissionsComponent: PermissionsComponent
}

class DefaultChannelComponent(
    componentContext: ComponentContext,
    chatId: String,
    repository: ChatRepository,
    currentUser: UserProfile
) : DefaultChatComponent(componentContext, chatId, repository, currentUser), ChannelComponent {

    override val participantsComponent: ParticipantsComponent =
        DefaultParticipantsComponent(componentContext, chatId, repository)
    override val permissionsComponent: PermissionsComponent =
        DefaultPermissionsComponent(componentContext, chatId, repository)
}

@Composable
fun ChannelContent(
    component: ChannelComponent,
    mediaAttachmentComponent: MediaAttachmentComponent,
    modifier: Modifier = Modifier
) {
    val state by component.state.collectAsState()
    val summary by component.summary.collectAsState()
    Column(modifier = modifier.fillMaxSize()) {
        ChatContent(
            state = state,
            title = summary?.title ?: "Channel",
            onIntent = component::onIntent,
            mediaAttachmentComponent = mediaAttachmentComponent,
            modifier = Modifier.weight(1f)
        )
        if (state is com.example.testchat.feature.chat.common.ChatState.Loaded && !(state as com.example.testchat.feature.chat.common.ChatState.Loaded).canSend) {
            Text(
                text = "You cannot post in this channel",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp)
            )
        }
        Divider()
        ParticipantsContent(component = component.participantsComponent)
        PermissionsContent(component = component.permissionsComponent)
    }
}
