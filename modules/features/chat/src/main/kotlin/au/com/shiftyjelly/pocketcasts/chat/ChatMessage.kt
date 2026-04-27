package au.com.shiftyjelly.pocketcasts.chat

import au.com.shiftyjelly.pocketcasts.models.entity.EpisodeChatMessage
import com.squareup.moshi.JsonAdapter
import java.util.UUID

sealed interface ChatMessage {
    val uuid: String
    val role: ChatRole

    data class User(
        val text: String,
        override val uuid: String = UUID.randomUUID().toString(),
    ) : ChatMessage {
        override val role: ChatRole get() = ChatRole.User
    }

    data class Assistant(
        val text: String,
        override val uuid: String = UUID.randomUUID().toString(),
    ) : ChatMessage {
        override val role: ChatRole get() = ChatRole.Assistant

        // Replace `- ` / `* ` bullet markers from the model with a styled bullet glyph.
        val displayText: String by lazy {
            Regex("""(?m)^\s*[-*]\s+""").replace(text, "•  ")
        }
    }

    data class Quote(
        val text: String,
        val start: String,
        val end: String,
        val startMs: Int = -1,
        val endMs: Int = -1,
        val canPlay: Boolean = false,
        val isPlaying: Boolean = false,
        override val uuid: String = UUID.randomUUID().toString(),
    ) : ChatMessage {
        override val role: ChatRole get() = ChatRole.Quote

        // Empty when there's nothing meaningful to display (both strings missing/blank).
        val timestampLabel: String
            get() = if (start.isNotBlank() && end.isNotBlank()) "$start – $end" else ""

        // Strip any straight or smart quotes the model wrapped around the text and re-add curly quotes.
        val displayText: String by lazy {
            "“${text.trim('"', '“', '”', ' ')}”"
        }
    }
}

enum class ChatRole(val value: String, val apiRole: String) {
    Assistant(value = "assistant", apiRole = "assistant"),
    User(value = "user", apiRole = "user"),
    Quote(value = "quote", apiRole = "assistant"),
}

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
        uuid = uuid,
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
        ChatRole.User.value -> ChatMessage.User(text = entity.text, uuid = entity.uuid)
        ChatRole.Quote.value -> entity.toQuoteMessageOrNull(quoteMetadataAdapter)
        else -> ChatMessage.Assistant(text = entity.text, uuid = entity.uuid)
    }
}

private fun EpisodeChatMessage.toQuoteMessageOrNull(
    quoteMetadataAdapter: JsonAdapter<QuoteMetadata>,
): ChatMessage.Quote? {
    val raw = metadata ?: return null
    val parsed = runCatching { quoteMetadataAdapter.fromJson(raw) }.getOrNull() ?: return null
    return ChatMessage.Quote(
        text = text,
        start = parsed.start,
        end = parsed.end,
        startMs = parseTimestampMs(parsed.start) ?: -1,
        endMs = parseTimestampMs(parsed.end) ?: -1,
        uuid = uuid,
    )
}
