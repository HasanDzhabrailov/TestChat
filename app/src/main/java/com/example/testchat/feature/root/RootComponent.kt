package com.example.testchat.feature.root

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.replaceCurrent
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.extensions.coroutines.componentCoroutineScope
import com.arkivanov.decompose.extensions.compose.jetbrains.subscribeAsState
import com.example.testchat.core.data.ChatRepository
import com.example.testchat.core.media.MediaAttachmentComponent
import com.example.testchat.core.model.ChatType
import com.example.testchat.core.model.UserProfile
import com.example.testchat.feature.chat.channel.ChannelContent
import com.example.testchat.feature.chat.channel.ChannelComponent
import com.example.testchat.feature.chat.channel.DefaultChannelComponent
import com.example.testchat.feature.chat.common.ChatContent
import com.example.testchat.feature.chat.common.DefaultChatComponent
import com.example.testchat.feature.chat.group.DefaultGroupChatComponent
import com.example.testchat.feature.chat.group.GroupChatComponent
import com.example.testchat.feature.chat.group.GroupChatContent
import com.example.testchat.feature.chats.list.ChatsListContent
import com.example.testchat.feature.chats.list.DefaultChatsListComponent
import com.example.testchat.feature.settings.DefaultSettingsComponent
import com.example.testchat.feature.settings.SettingsComponent
import com.example.testchat.feature.settings.SettingsContent
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import com.arkivanov.essenty.parcelable.Parcelable

interface RootComponent {
    val childStack: Value<ChildStack<Configuration, Child>>
    fun navigateToChat(chatId: String)
    fun navigateToSettings()
    fun back()

    sealed interface Child {
        data class ChatsList(val component: DefaultChatsListComponent) : Child
        data class Chat(val component: DefaultChatComponent) : Child
        data class Group(val component: GroupChatComponent) : Child
        data class Channel(val component: ChannelComponent) : Child
        data class Settings(val component: SettingsComponent) : Child
    }

    @Parcelize
    sealed class Configuration : Parcelable {
        data object ChatsList : Configuration()
        data class Chat(val chatId: String, val type: ChatType) : Configuration()
        data object Settings : Configuration()
    }
}

class DefaultRootComponent(
    componentContext: ComponentContext,
    private val repository: ChatRepository,
    private val mediaAttachmentComponent: MediaAttachmentComponent,
    private val currentUser: UserProfile
) : RootComponent, ComponentContext by componentContext {

    private val navigation = StackNavigation<RootComponent.Configuration>()
    private val scope = componentCoroutineScope()

    override val childStack: Value<ChildStack<RootComponent.Configuration, RootComponent.Child>> = childStack(
        source = navigation,
        initialConfiguration = RootComponent.Configuration.ChatsList,
        handleBackButton = true,
        childFactory = ::createChild
    )

    private fun createChild(config: RootComponent.Configuration, context: ComponentContext): RootComponent.Child =
        when (config) {
            RootComponent.Configuration.ChatsList -> RootComponent.Child.ChatsList(
                DefaultChatsListComponent(
                    componentContext = context,
                    repository = repository,
                    navigateToChat = ::navigateToChat,
                    navigateToSettings = ::navigateToSettings
                )
            )

            is RootComponent.Configuration.Chat -> when (config.type) {
                ChatType.DIRECT -> RootComponent.Child.Chat(
                    DefaultChatComponent(context, config.chatId, repository, currentUser)
                )

                ChatType.GROUP -> RootComponent.Child.Group(
                    DefaultGroupChatComponent(context, config.chatId, repository, currentUser)
                )

                ChatType.CHANNEL -> RootComponent.Child.Channel(
                    DefaultChannelComponent(context, config.chatId, repository, currentUser)
                )
            }

            RootComponent.Configuration.Settings -> RootComponent.Child.Settings(
                DefaultSettingsComponent(context) { back() }
            )
        }

    override fun navigateToChat(chatId: String) {
        scope.launch {
            val type = repository.getChatType(chatId)
            navigation.replaceCurrent(RootComponent.Configuration.Chat(chatId, type))
        }
    }

    override fun navigateToSettings() {
        navigation.replaceCurrent(RootComponent.Configuration.Settings)
    }

    override fun back() {
        navigation.replaceCurrent(RootComponent.Configuration.ChatsList)
    }

    @Composable
    fun RootContent(modifier: Modifier = Modifier) {
        RootContent(this, mediaAttachmentComponent, modifier)
    }
}

@Composable
fun RootContent(component: RootComponent, mediaAttachmentComponent: MediaAttachmentComponent, modifier: Modifier = Modifier) {
    val stack by component.childStack.subscribeAsState()
    BackHandler(enabled = stack.active.configuration != RootComponent.Configuration.ChatsList) {
        component.back()
    }
    com.arkivanov.decompose.extensions.compose.jetbrains.stack.Children(
        stack = component.childStack,
        modifier = modifier
    ) { child ->
        when (val instance = child.instance) {
            is RootComponent.Child.ChatsList -> ChatsListContent(instance.component)
            is RootComponent.Child.Chat -> ChatContent(instance.component, mediaAttachmentComponent)
            is RootComponent.Child.Group -> GroupChatContent(instance.component, mediaAttachmentComponent)
            is RootComponent.Child.Channel -> ChannelContent(instance.component, mediaAttachmentComponent)
            is RootComponent.Child.Settings -> SettingsContent(instance.component)
        }
    }
}
