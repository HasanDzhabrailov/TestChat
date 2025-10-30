package com.example.testchat.core.model

import androidx.compose.runtime.Immutable

@Immutable
data class PermissionsSnapshot(
    val roles: Map<String, ParticipantRole>,
    val adminIds: Set<String>,
    val restrictions: Set<String>
) {
    fun canPost(userId: String): Boolean = when (roles[userId]) {
        ParticipantRole.OWNER, ParticipantRole.ADMIN, ParticipantRole.MODERATOR -> true
        ParticipantRole.MEMBER -> !restrictions.contains("POST_BLOCKED")
        ParticipantRole.READER, null -> false
    }
}
