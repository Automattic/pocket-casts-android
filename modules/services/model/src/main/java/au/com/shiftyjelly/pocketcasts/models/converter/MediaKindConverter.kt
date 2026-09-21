package au.com.shiftyjelly.pocketcasts.models.converter

import androidx.room.TypeConverter
import au.com.shiftyjelly.pocketcasts.models.type.MediaKind

class MediaKindConverter {

    @TypeConverter
    fun toMediaKind(value: String?): MediaKind? {
        return MediaKind.fromServer(value)
    }

    @TypeConverter
    fun toString(mediaKind: MediaKind?): String? {
        return mediaKind?.stringValue
    }
}
