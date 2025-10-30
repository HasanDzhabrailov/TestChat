package com.example.testchat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.arkivanov.decompose.defaultComponentContext
import com.example.testchat.core.media.MediaAttachmentComponent
import com.example.testchat.feature.root.RootComponent
import com.example.testchat.feature.root.RootComponentFactory
import com.example.testchat.feature.root.RootContent
import com.example.testchat.ui.theme.TestChatTheme
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val rootComponentFactory: RootComponentFactory by inject()
    private val mediaAttachmentComponent: MediaAttachmentComponent by inject()

    private lateinit var rootComponent: RootComponent

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        rootComponent = rootComponentFactory.create(defaultComponentContext())
        setContent {
            TestChatTheme {
                RootContent(rootComponent, mediaAttachmentComponent)
            }
        }
    }
}
