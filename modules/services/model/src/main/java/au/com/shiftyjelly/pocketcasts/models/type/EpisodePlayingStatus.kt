package au.com.shiftyjelly.pocketcasts.models.type

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter

enum class EpisodePlayingStatus {
    NOT_PLAYED,
    IN_PROGRESS,
    COMPLETED,
    ;

    fun toInt(): Int = when (this) {
        NOT_PLAYED -> 1
        IN_PROGRESS -> 2
        COMPLETED -> 3
    }

    companion object {
        fun fromInt(n: Int) =
            when (n) {
                1 -> NOT_PLAYED
                2 -> IN_PROGRESS
                3 -> COMPLETED
                else -> NOT_PLAYED
            }
    }
}

class EpisodePlayingStatusMoshiAdapter : JsonAdapter<EpisodePlayingStatus>() {
    override fun toJson(writer: JsonWriter, value: EpisodePlayingStatus?) {}

    override fun fromJson(reader: JsonReader): EpisodePlayingStatus? {
        val value = reader.nextInt()
        return EpisodePlayingStatus.fromInt(value)
    }
}
