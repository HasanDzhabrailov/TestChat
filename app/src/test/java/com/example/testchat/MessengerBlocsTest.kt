package com.example.testchat

import app.cash.turbine.test
import com.example.testchat.core.data.InMemoryChatRepository
import com.example.testchat.core.model.ParticipantRole
import com.example.testchat.core.model.UserProfile
import com.example.testchat.feature.chat.common.ChatBloc
import com.example.testchat.feature.chat.common.ChatIntent
import com.example.testchat.feature.chat.common.ChatState
import com.example.testchat.feature.chat.message.MessageComponent
import com.example.testchat.feature.chat.participants.ParticipantsBloc
import com.example.testchat.feature.chat.participants.ParticipantsIntent
import com.example.testchat.feature.chat.permissions.PermissionsBloc
import com.example.testchat.feature.chat.permissions.PermissionsIntent
import com.example.testchat.feature.chats.list.ChatsListBloc
import com.example.testchat.feature.chats.list.ChatsListIntent
import com.example.testchat.feature.chats.list.ChatsListState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class MessengerBlocsTest {

    private val currentUser = UserProfile(id = "tester", displayName = "Test User")

    @Test
    fun `chats list bloc emits loaded state`() = runTest {
        val repository = InMemoryChatRepository(currentUser)
        val bloc = ChatsListBloc(repository, backgroundScope)

        bloc.dispatch(ChatsListIntent.LoadChats)
        advanceUntilIdle()

        bloc.state.test {
            assertTrue(awaitItem() is ChatsListState.Loading)
            val loaded = awaitItem()
            assertTrue(loaded is ChatsListState.Loaded)
            assertEquals(3, loaded.chats.size)
        }
    }

    @Test
    fun `chat bloc sends message`() = runTest {
        val repository = InMemoryChatRepository(currentUser)
        val bloc = ChatBloc("chat-direct", currentUser, repository, backgroundScope)

        bloc.dispatch(ChatIntent.LoadMessages)
        advanceUntilIdle()

        bloc.state.test {
            assertTrue(awaitItem() is ChatState.Loading)
            val initial = awaitItem() as ChatState.Loaded
            bloc.dispatch(ChatIntent.Send(MessageComponent.TextMessage("Hello")))
            advanceUntilIdle()
            val updated = awaitItem() as ChatState.Loaded
            assertEquals(initial.messages.size + 1, updated.messages.size)
        }
    }

    @Test
    fun `participants bloc removes participant`() = runTest {
        val repository = InMemoryChatRepository(currentUser)
        val bloc = ParticipantsBloc("chat-group", repository, backgroundScope)

        bloc.dispatch(ParticipantsIntent.LoadParticipants)
        advanceUntilIdle()

        bloc.state.test {
            assertTrue(awaitItem().isLoading)
            val loaded = awaitItem()
            val initialCount = loaded.participants.size
            val toRemove = loaded.participants.last()
            bloc.dispatch(ParticipantsIntent.RemoveUser(toRemove.id))
            advanceUntilIdle()
            val updated = awaitItem()
            assertEquals(initialCount - 1, updated.participants.size)
        }
    }

    @Test
    fun `permissions bloc promotes participant`() = runTest {
        val repository = InMemoryChatRepository(currentUser)
        val bloc = PermissionsBloc("chat-group", repository, backgroundScope)

        bloc.dispatch(PermissionsIntent.LoadPermissions)
        advanceUntilIdle()

        bloc.state.test {
            assertTrue(awaitItem().isLoading)
            val loaded = awaitItem()
            val targetId = loaded.roles.entries.first { it.value == ParticipantRole.MEMBER }.key
            bloc.dispatch(PermissionsIntent.Promote(targetId))
            advanceUntilIdle()
            val updated = awaitItem()
            assertTrue(updated.roles[targetId] == ParticipantRole.ADMIN || updated.roles[targetId] == ParticipantRole.MODERATOR)
        }
    }
}
