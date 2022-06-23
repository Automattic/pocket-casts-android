package au.com.shiftyjelly.pocketcasts.models.converter

import androidx.room.TypeConverter
import au.com.shiftyjelly.pocketcasts.models.type.EpisodePlayingStatus

class EpisodePlayingStatusConverter {

    @TypeConverter
    fun toEpisodePlayingStatus(value: Int?): EpisodePlayingStatus? {
        return if (value == null) null else EpisodePlayingStatus.values()[value]
    }

    @TypeConverter
    fun toInt(value: EpisodePlayingStatus?): Int? {
        return value?.ordinal
    }
}
