package au.com.shiftyjelly.pocketcasts.models.converter

import androidx.room.TypeConverter
import au.com.shiftyjelly.pocketcasts.models.to.AutoArchiveAfterPlaying

class AutoArchiveAfterPlayingTypeConverter {
    @TypeConverter
    fun fromInt(value: Int?): AutoArchiveAfterPlaying {
        return value?.let { AutoArchiveAfterPlaying.fromIndex(it) } ?: AutoArchiveAfterPlaying.Never
    }

    @TypeConverter
    fun toInt(value: AutoArchiveAfterPlaying?): Int {
        return value?.index ?: AutoArchiveAfterPlaying.Never.index
    }
}
