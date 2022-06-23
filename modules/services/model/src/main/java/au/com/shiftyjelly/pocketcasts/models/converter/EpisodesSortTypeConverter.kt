package au.com.shiftyjelly.pocketcasts.models.converter

import androidx.room.TypeConverter
import au.com.shiftyjelly.pocketcasts.models.type.EpisodesSortType

class EpisodesSortTypeConverter {

    @TypeConverter
    fun toEpisodesSortType(value: Int): EpisodesSortType {
        return EpisodesSortType.values().getOrNull(value) ?: EpisodesSortType.EPISODES_SORT_BY_DATE_DESC
    }

    @TypeConverter
    fun toInt(value: EpisodesSortType): Int {
        return value.ordinal
    }
}
