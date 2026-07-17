package au.com.shiftyjelly.pocketcasts.models.converter

import com.squareup.moshi.Json
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import javax.inject.Inject

@JsonClass(generateAdapter = true)
data class TranscriptSegments(
    @Json(name = "segments") val segments: List<TranscriptCue> = emptyList(),
    // Flightcast transcripts embed a WEBVTT document alongside the segments.
    @Json(name = "vtt") val vtt: String? = null,
)

@JsonClass(generateAdapter = true)
data class TranscriptCue(
    // Podcast Index transcripts use "body"; Flightcast transcripts use "text".
    @Json(name = "body") val body: String? = null,
    @Json(name = "text") val text: String? = null,
    // Podcast Index uses "startTime"/"endTime"; Flightcast uses "start"/"end".
    @Json(name = "startTime") val startTime: Double? = null,
    @Json(name = "endTime") val endTime: Double? = null,
    @Json(name = "start") val start: Double? = null,
    @Json(name = "end") val end: Double? = null,
    @Json(name = "speaker") val speaker: String? = null,
) {
    val content: String? get() = body ?: text
    val startSeconds: Double? get() = startTime ?: start
    val endSeconds: Double? get() = endTime ?: end
}

class TranscriptJsonConverter @Inject constructor(
    private val moshi: Moshi,
) {
    fun fromString(jsonString: String): List<TranscriptCue> {
        val jsonAdapter: JsonAdapter<TranscriptSegments> =
            moshi.adapter(TranscriptSegments::class.java)
        val transcriptJson =
            jsonAdapter.fromJson(jsonString) ?: throw IllegalArgumentException("Invalid JSON")

        return transcriptJson.segments
    }
}
