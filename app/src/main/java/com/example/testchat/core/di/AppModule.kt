package com.example.testchat.core.di

import com.example.testchat.core.data.ChatRepository
import com.example.testchat.core.data.InMemoryChatRepository
import com.example.testchat.core.media.DefaultMediaAttachmentComponent
import com.example.testchat.core.media.MediaAttachmentComponent
import com.example.testchat.core.model.UserProfile
import com.example.testchat.feature.root.DefaultRootComponent
import com.example.testchat.feature.root.RootComponent
import com.example.testchat.feature.root.RootComponentFactory
import kotlinx.serialization.json.Json
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val appModule = module {
    single {
        Json {
            ignoreUnknownKeys = true
            isLenient = true
        }
    }
    single { UserProfile(id = "me", displayName = "Taylor Kim") }
    single<ChatRepository> { InMemoryChatRepository(get()) }
    single<MediaAttachmentComponent> { DefaultMediaAttachmentComponent(androidContext(), get()) }
    single<RootComponentFactory> {
        val repository: ChatRepository = get()
        val media: MediaAttachmentComponent = get()
        val user: UserProfile = get()
        RootComponentFactory { componentContext ->
            DefaultRootComponent(
                componentContext = componentContext,
                repository = repository,
                mediaAttachmentComponent = media,
                currentUser = user
            )
        }
    }
}
