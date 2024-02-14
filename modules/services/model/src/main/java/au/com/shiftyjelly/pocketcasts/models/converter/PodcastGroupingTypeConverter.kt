package au.com.shiftyjelly.pocketcasts.models.converter

import androidx.room.TypeConverter
import au.com.shiftyjelly.pocketcasts.models.to.PodcastGrouping

class PodcastGroupingTypeConverter {
    @TypeConverter
    fun toTrimMode(value: Int?): PodcastGrouping {
        return value?.let { PodcastGrouping.All.getOrNull(it) } ?: PodcastGrouping.None
    }

    @TypeConverter
    fun toInt(value: PodcastGrouping?): Int {
        return value?.let { PodcastGrouping.All.indexOf(it) } ?: 0
    }
}
