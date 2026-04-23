package au.com.shiftyjelly.pocketcasts.chat

import au.com.shiftyjelly.pocketcasts.models.entity.EpisodeChatMessage
import java.util.UUID

data class ChatMessage(
    val text: String,
    val role: ChatRole,
)

enum class ChatRole {
    Ai,
    User,
}

fun ChatMessage.toEntity(episodeUuid: String) = EpisodeChatMessage(
    uuid = UUID.randomUUID().toString(),
    episodeUuid = episodeUuid,
    text = text,
    role = role.name.lowercase(),
)

fun List<EpisodeChatMessage>.toChatMessages() = map { entity ->
    ChatMessage(
        text = entity.text,
        role = when (entity.role) {
            "ai" -> ChatRole.Ai
            else -> ChatRole.User
        },
    )
}
