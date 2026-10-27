package au.com.shiftyjelly.pocketcasts.repositories.chat

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class AssistantMessageMetadata(
    val followUps: List<String> = emptyList(),
)
