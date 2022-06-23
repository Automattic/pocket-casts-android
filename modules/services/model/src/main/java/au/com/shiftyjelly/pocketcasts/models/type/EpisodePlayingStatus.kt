package au.com.shiftyjelly.pocketcasts.models.type

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter

enum class EpisodePlayingStatus {
    NOT_PLAYED,
    IN_PROGRESS,
    COMPLETED
}

class EpisodePlayingStatusMoshiAdapter : JsonAdapter<EpisodePlayingStatus>() {
    override fun toJson(writer: JsonWriter, value: EpisodePlayingStatus?) {}

    override fun fromJson(reader: JsonReader): EpisodePlayingStatus? {
        val value = reader.nextInt()
        return when (value) {
            1 -> EpisodePlayingStatus.NOT_PLAYED
            2 -> EpisodePlayingStatus.IN_PROGRESS
            3 -> EpisodePlayingStatus.COMPLETED
            else -> EpisodePlayingStatus.NOT_PLAYED
        }
    }
}
