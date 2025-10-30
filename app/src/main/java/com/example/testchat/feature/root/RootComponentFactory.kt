package com.example.testchat.feature.root

import com.arkivanov.decompose.ComponentContext

fun interface RootComponentFactory {
    fun create(componentContext: ComponentContext): RootComponent
}
