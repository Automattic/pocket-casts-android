package au.com.shiftyjelly.pocketcasts.chat

import au.com.shiftyjelly.pocketcasts.models.entity.EpisodeChatMessage
import java.util.UUID

data class ChatMessage(
    val text: String,
    val role: ChatRole,
    val metadata: String? = null,
)

enum class ChatRole(val value: String, val apiRole: String) {
    Assistant(value = "assistant", apiRole = "assistant"),
    User(value = "user", apiRole = "user"),
    Quote(value = "quote", apiRole = "assistant"),
}

fun ChatMessage.toEntity(episodeUuid: String) = EpisodeChatMessage(
    uuid = UUID.randomUUID().toString(),
    episodeUuid = episodeUuid,
    text = text,
    role = role.value,
    metadata = metadata,
)

fun List<EpisodeChatMessage>.toChatMessages() = map { entity ->
    ChatMessage(
        text = entity.text,
        role = when (entity.role) {
            ChatRole.User.value -> ChatRole.User
            ChatRole.Quote.value -> ChatRole.Quote
            else -> ChatRole.Assistant
        },
        metadata = entity.metadata,
    )
}
