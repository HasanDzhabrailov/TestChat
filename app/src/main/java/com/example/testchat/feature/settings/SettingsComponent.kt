package com.example.testchat.feature.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.ComponentContext

interface SettingsComponent {
    fun onClose()
}

class DefaultSettingsComponent(
    private val componentContext: ComponentContext,
    private val onBack: () -> Unit
) : SettingsComponent {
    override fun onClose() = onBack()
}

@Composable
fun SettingsContent(component: SettingsComponent, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(title = { Text("Settings") })
        Column(modifier = Modifier.padding(24.dp)) {
            Text("Notifications", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "Toggle push notifications, message previews and sound.",
                style = MaterialTheme.typography.bodyMedium
            )
            Text("Appearance", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 24.dp))
            Text("Customize your theme, accent color and chat wallpaper.", style = MaterialTheme.typography.bodyMedium)
        }
    }
}
