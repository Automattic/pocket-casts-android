package au.com.shiftyjelly.pocketcasts.models.converter

import androidx.room.TypeConverter
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast

class PodcastLicensingEnumConverter {

    @TypeConverter
    fun toLicensing(value: Int?): Podcast.Licensing? {
        return if (value == null) null else Podcast.Licensing.values()[value]
    }

    @TypeConverter
    fun toInt(value: Podcast.Licensing?): Int? {
        return value?.ordinal
    }

    @TypeConverter
    fun listToInt(value: List<Podcast.Licensing>): List<Int> {
        return value.mapNotNull { toInt(it) }
    }

    @TypeConverter
    fun listToEpisodeStatus(value: List<Int>): List<Podcast.Licensing> {
        return value.mapNotNull { toLicensing(it) }
    }
}
