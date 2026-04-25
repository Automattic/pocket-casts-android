package au.com.shiftyjelly.pocketcasts.chat

import au.com.shiftyjelly.pocketcasts.models.entity.EpisodeChatMessage
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonClass
import java.util.UUID

sealed interface ChatMessage {
    val role: ChatRole

    data class User(val text: String) : ChatMessage {
        override val role: ChatRole get() = ChatRole.User
    }

    data class Assistant(val text: String) : ChatMessage {
        override val role: ChatRole get() = ChatRole.Assistant
    }

    data class Quote(val text: String, val start: String, val end: String) : ChatMessage {
        override val role: ChatRole get() = ChatRole.Quote
    }
}

enum class ChatRole(val value: String, val apiRole: String) {
    Assistant(value = "assistant", apiRole = "assistant"),
    User(value = "user", apiRole = "user"),
    Quote(value = "quote", apiRole = "assistant"),
}

@JsonClass(generateAdapter = true)
internal data class QuoteMetadata(val start: String, val end: String)

// Text content for history payloads. Returns null for message types that carry no text.
fun ChatMessage.textOrNull(): String? = when (this) {
    is ChatMessage.User -> text
    is ChatMessage.Assistant -> text
    is ChatMessage.Quote -> text
}

internal fun ChatMessage.toEntity(
    episodeUuid: String,
    quoteMetadataAdapter: JsonAdapter<QuoteMetadata>,
): EpisodeChatMessage {
    val (text, metadata) = when (this) {
        is ChatMessage.User -> text to null
        is ChatMessage.Assistant -> text to null
        is ChatMessage.Quote -> text to quoteMetadataAdapter.toJson(QuoteMetadata(start, end))
    }
    return EpisodeChatMessage(
        uuid = UUID.randomUUID().toString(),
        episodeUuid = episodeUuid,
        text = text,
        role = role.value,
        metadata = metadata,
    )
}

internal fun List<EpisodeChatMessage>.toChatMessages(
    quoteMetadataAdapter: JsonAdapter<QuoteMetadata>,
) = mapNotNull { entity ->
    when (entity.role) {
        ChatRole.User.value -> ChatMessage.User(text = entity.text)
        ChatRole.Quote.value -> entity.toQuoteMessageOrNull(quoteMetadataAdapter)
        else -> ChatMessage.Assistant(text = entity.text)
    }
}

private fun EpisodeChatMessage.toQuoteMessageOrNull(
    quoteMetadataAdapter: JsonAdapter<QuoteMetadata>,
): ChatMessage.Quote? {
    val raw = metadata ?: return null
    val parsed = runCatching { quoteMetadataAdapter.fromJson(raw) }.getOrNull() ?: return null
    return ChatMessage.Quote(text = text, start = parsed.start, end = parsed.end)
}
