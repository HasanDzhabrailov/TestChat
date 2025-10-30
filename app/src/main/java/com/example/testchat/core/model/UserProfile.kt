package com.example.testchat.core.model

import androidx.compose.runtime.Immutable

@Immutable
data class UserProfile(
    val id: String,
    val displayName: String,
    val avatarUrl: String? = null
)
