package au.com.shiftyjelly.pocketcasts.models.converter

import com.squareup.moshi.Json
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import javax.inject.Inject

@JsonClass(generateAdapter = true)
data class TranscriptSegments(
    @Json(name = "segments") val segments: List<TranscriptCue>,
)

@JsonClass(generateAdapter = true)
data class TranscriptCue(
    @Json(name = "body") val body: String,
    @Json(name = "startTime") val startTime: Double?,
    @Json(name = "endTime") val endTime: Double?,
    @Json(name = "speaker") val speaker: String?,
)

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
