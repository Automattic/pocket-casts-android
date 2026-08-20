package au.com.shiftyjelly.pocketcasts.models.type

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter

sealed class MediaKind(val stringValue: String) {
    companion object {
        private const val VIDEO = "video"
        private const val AUDIO = "audio"
        private const val YOUTUBE = "youtube"
        private const val VIMEO = "vimeo"
        private const val OTHER = "other"

        fun fromServer(value: String?): MediaKind? = when (value) {
            null -> null
            VIDEO -> Video
            AUDIO -> Audio
            YOUTUBE -> YouTube
            VIMEO -> Vimeo
            OTHER -> Other
            else -> Unknown(value)
        }
    }

    data object Video : MediaKind(VIDEO)
    data object Audio : MediaKind(AUDIO)
    data object YouTube : MediaKind(YOUTUBE)
    data object Vimeo : MediaKind(VIMEO)

    // The server's own catch-all, which is not the same as a kind this version of the app doesn't know yet.
    data object Other : MediaKind(OTHER)

    data class Unknown(val value: String) : MediaKind(value)

    override fun toString() = stringValue
}

class MediaKindMoshiAdapter : JsonAdapter<MediaKind>() {
    override fun fromJson(reader: JsonReader): MediaKind? {
        return MediaKind.fromServer(reader.nextString())
    }

    override fun toJson(writer: JsonWriter, mediaKind: MediaKind?) {
        writer.value(mediaKind?.stringValue)
    }
}
