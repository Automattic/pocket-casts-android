package au.com.shiftyjelly.pocketcasts.player.view.transcripts

import com.squareup.moshi.Json
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import javax.inject.Inject

@JsonClass(generateAdapter = true)
data class TranscriptSegments(
    @field:Json(name = "segments") var segments: List<TranscriptCue>,
)

@JsonClass(generateAdapter = true)
data class TranscriptCue(
    @field:Json(name = "body") var body: String?,
    @field:Json(name = "startTime") var startTime: Double?,
    @field:Json(name = "endTime") var endTime: Double?,
    @field:Json(name = "speaker") var speaker: String?,
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
