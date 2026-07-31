package au.com.shiftyjelly.pocketcasts.repositories.chat

import com.squareup.moshi.JsonClass

// Persisted alongside a [ChatMessage.Quote] in [EpisodeChatMessage.metadata] as JSON.
@JsonClass(generateAdapter = true)
internal data class QuoteMetadata(val start: String, val end: String)
