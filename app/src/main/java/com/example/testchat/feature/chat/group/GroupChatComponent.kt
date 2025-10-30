package com.example.testchat.feature.chat.group

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.weight
import androidx.compose.material3.Divider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.ComponentContext
import com.example.testchat.core.data.ChatRepository
import com.example.testchat.core.media.MediaAttachmentComponent
import com.example.testchat.core.model.UserProfile
import com.example.testchat.feature.chat.common.ChatComponent
import com.example.testchat.feature.chat.common.ChatContent
import com.example.testchat.feature.chat.common.DefaultChatComponent
import com.example.testchat.feature.chat.participants.DefaultParticipantsComponent
import com.example.testchat.feature.chat.participants.ParticipantsComponent
import com.example.testchat.feature.chat.participants.ParticipantsContent
import com.example.testchat.feature.chat.permissions.DefaultPermissionsComponent
import com.example.testchat.feature.chat.permissions.PermissionsComponent
import com.example.testchat.feature.chat.permissions.PermissionsContent

interface GroupChatComponent : ChatComponent {
    val participantsComponent: ParticipantsComponent
    val permissionsComponent: PermissionsComponent
}

class DefaultGroupChatComponent(
    componentContext: ComponentContext,
    chatId: String,
    repository: ChatRepository,
    currentUser: UserProfile
) : DefaultChatComponent(componentContext, chatId, repository, currentUser), GroupChatComponent {

    override val participantsComponent: ParticipantsComponent =
        DefaultParticipantsComponent(componentContext, chatId, repository)
    override val permissionsComponent: PermissionsComponent =
        DefaultPermissionsComponent(componentContext, chatId, repository)
}

@Composable
fun GroupChatContent(
    component: GroupChatComponent,
    mediaAttachmentComponent: MediaAttachmentComponent,
    modifier: Modifier = Modifier
) {
    val chatState by component.state.collectAsState()
    val summary by component.summary.collectAsState()
    Column(modifier = modifier.fillMaxSize()) {
        ChatContent(
            state = chatState,
            title = summary?.title ?: "Group",
            onIntent = component::onIntent,
            mediaAttachmentComponent = mediaAttachmentComponent,
            modifier = Modifier.weight(1f)
        )
        Divider()
        ParticipantsContent(component = component.participantsComponent)
        PermissionsContent(component = component.permissionsComponent)
    }
}
