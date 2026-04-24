package au.com.shiftyjelly.pocketcasts.servers.podcast

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class EpisodeChatRequest(
    @Json(name = "episode_uuid") val episodeUuid: String? = null,
    @Json(name = "podcast_uuid") val podcastUuid: String? = null,
    @Json(name = "transcript_url") val transcriptUrl: String? = null,
    @Json(name = "message") val message: String,
    @Json(name = "conversation_history") val conversationHistory: List<ConversationMessage>,
)

@JsonClass(generateAdapter = true)
data class ConversationMessage(
    @Json(name = "role") val role: String,
    @Json(name = "content") val content: String,
)

@JsonClass(generateAdapter = true)
data class EpisodeChatResponse(
    @Json(name = "reply") val reply: String,
    @Json(name = "quote") val quote: String?,
    @Json(name = "quote_time") val quoteTime: String?,
)
