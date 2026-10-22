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

    // Must stay final so the data objects and Unknown don't generate a toString that hides the server value.
    final override fun toString() = stringValue
}

class MediaKindMoshiAdapter : JsonAdapter<MediaKind>() {
    override fun fromJson(reader: JsonReader): MediaKind? {
        // A kind that isn't a string at all is treated as absent rather than failing the whole response.
        if (reader.peek() != JsonReader.Token.STRING) {
            reader.skipValue()
            return null
        }
        return MediaKind.fromServer(reader.nextString())
    }

    override fun toJson(writer: JsonWriter, mediaKind: MediaKind?) {
        writer.value(mediaKind?.stringValue)
    }
}
