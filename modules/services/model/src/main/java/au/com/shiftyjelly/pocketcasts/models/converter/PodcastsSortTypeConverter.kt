package au.com.shiftyjelly.pocketcasts.models.converter

import androidx.room.TypeConverter
import au.com.shiftyjelly.pocketcasts.models.type.PodcastsSortType

class PodcastsSortTypeConverter {

    @TypeConverter
    fun toPodcastsSortType(value: Int): PodcastsSortType {
        return PodcastsSortType.fromServerId(value)
    }

    @TypeConverter
    fun toInt(value: PodcastsSortType): Int {
        return value.serverId
    }
}
