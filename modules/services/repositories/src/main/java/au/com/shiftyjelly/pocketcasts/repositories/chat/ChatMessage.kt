package au.com.shiftyjelly.pocketcasts.repositories.chat

import au.com.shiftyjelly.pocketcasts.models.entity.EpisodeChatMessage
import com.squareup.moshi.JsonAdapter
import java.util.UUID

sealed interface ChatMessage {
    val uuid: String
    val role: ChatRole

    data class User(
        val text: String,
        val failure: ChatSendFailure? = null,
        val inputType: ChatInputType = ChatInputType.Typed,
        override val uuid: String = UUID.randomUUID().toString(),
    ) : ChatMessage {
        override val role: ChatRole get() = ChatRole.User
    }

    data class Assistant(
        val text: String,
        val rating: ChatAnswerRating? = null,
        val followUps: List<String> = emptyList(),
        override val uuid: String = UUID.randomUUID().toString(),
    ) : ChatMessage {
        override val role: ChatRole get() = ChatRole.Assistant

        // Replace `- ` / `* ` bullet markers from the model with a styled bullet glyph.
        val displayText: String by lazy {
            Regex("""(?m)^[ \t]*[-*][ \t]+""").replace(text, "•  ")
        }
    }

    data class Quote(
        val text: String,
        val start: String,
        val end: String,
        val startMs: Int = -1,
        val endMs: Int = -1,
        val canPlay: Boolean = false,
        val isFromGeneratedTranscript: Boolean = false,
        val rating: ChatAnswerRating? = null,
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

val ChatMessage.answerRating: ChatAnswerRating?
    get() = when (this) {
        is ChatMessage.User -> null
        is ChatMessage.Assistant -> rating
        is ChatMessage.Quote -> rating
    }

// Text content for history payloads. Returns null for message types that carry no text.
internal fun ChatMessage.textOrNull(): String? = when (this) {
    is ChatMessage.User -> text
    is ChatMessage.Assistant -> text
    is ChatMessage.Quote -> text
}

internal fun ChatMessage.toEntity(
    episodeUuid: String,
    quoteMetadataAdapter: JsonAdapter<QuoteMetadata>,
    userMetadataAdapter: JsonAdapter<UserMessageMetadata>,
    assistantMetadataAdapter: JsonAdapter<AssistantMessageMetadata>,
): EpisodeChatMessage {
    val (text, metadata) = when (this) {
        is ChatMessage.User -> text to userMetadataAdapter.toJsonOrNull(this)
        is ChatMessage.Assistant -> text to followUps.takeIf { it.isNotEmpty() }?.let {
            assistantMetadataAdapter.toJson(AssistantMessageMetadata(followUps = it))
        }
        is ChatMessage.Quote -> text to quoteMetadataAdapter.toJson(QuoteMetadata(start, end, isFromGeneratedTranscript))
    }
    return EpisodeChatMessage(
        uuid = uuid,
        episodeUuid = episodeUuid,
        text = text,
        role = role.value,
        metadata = metadata,
        rating = answerRating?.value,
    )
}

private fun JsonAdapter<UserMessageMetadata>.toJsonOrNull(message: ChatMessage.User): String? {
    val inputType = message.inputType.takeUnless { it == ChatInputType.Typed }
    if (message.failure == null && inputType == null) return null
    return toJson(UserMessageMetadata(failure = message.failure?.value, inputType = inputType?.value))
}

internal fun List<EpisodeChatMessage>.toChatMessages(
    quoteMetadataAdapter: JsonAdapter<QuoteMetadata>,
    userMetadataAdapter: JsonAdapter<UserMessageMetadata>,
    assistantMetadataAdapter: JsonAdapter<AssistantMessageMetadata>,
) = mapNotNull { entity ->
    when (entity.role) {
        ChatRole.User.value -> {
            val metadata = entity.metadata?.let { raw -> runCatching { userMetadataAdapter.fromJson(raw) }.getOrNull() }
            ChatMessage.User(
                text = entity.text,
                failure = ChatSendFailure.fromValue(metadata?.failure),
                inputType = ChatInputType.fromValue(metadata?.inputType) ?: ChatInputType.Typed,
                uuid = entity.uuid,
            )
        }

        ChatRole.Quote.value -> entity.toQuoteMessageOrNull(quoteMetadataAdapter)

        else -> ChatMessage.Assistant(
            text = entity.text,
            rating = ChatAnswerRating.fromValue(entity.rating),
            followUps = entity.metadata
                ?.let { raw -> runCatching { assistantMetadataAdapter.fromJson(raw) }.getOrNull() }
                ?.followUps
                .orEmpty(),
            uuid = entity.uuid,
        )
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
        isFromGeneratedTranscript = parsed.isFromGeneratedTranscript,
        rating = ChatAnswerRating.fromValue(rating),
        uuid = uuid,
    )
}
