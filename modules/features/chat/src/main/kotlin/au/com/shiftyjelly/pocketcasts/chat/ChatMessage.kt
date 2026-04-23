package au.com.shiftyjelly.pocketcasts.chat

import au.com.shiftyjelly.pocketcasts.models.entity.EpisodeChatMessage
import java.util.UUID

data class ChatMessage(
    val text: String,
    val role: ChatRole,
)

enum class ChatRole(val value: String) {
    Assistant("assistant"),
    User("user"),
}

fun ChatMessage.toEntity(episodeUuid: String) = EpisodeChatMessage(
    uuid = UUID.randomUUID().toString(),
    episodeUuid = episodeUuid,
    text = text,
    role = role.value,
)

fun List<EpisodeChatMessage>.toChatMessages() = map { entity ->
    ChatMessage(
        text = entity.text,
        role = when (entity.role) {
            ChatRole.Assistant.value -> ChatRole.Assistant
            else -> ChatRole.User
        },
    )
}
