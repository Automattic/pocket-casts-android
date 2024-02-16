package au.com.shiftyjelly.pocketcasts.models.converter

import androidx.room.TypeConverter
import au.com.shiftyjelly.pocketcasts.models.to.PodcastGrouping

class PodcastGroupingTypeConverter {
    @TypeConverter
    fun fromInt(value: Int?): PodcastGrouping {
        return value?.let { PodcastGrouping.fromIndex(it) } ?: PodcastGrouping.None
    }

    @TypeConverter
    fun toInt(value: PodcastGrouping?): Int {
        return value?.index ?: PodcastGrouping.None.index
    }
}
