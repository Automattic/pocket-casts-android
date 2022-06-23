package au.com.shiftyjelly.pocketcasts.models.converter

import androidx.room.TypeConverter
import au.com.shiftyjelly.pocketcasts.models.type.EpisodeStatusEnum

class EpisodeStatusEnumConverter {

    @TypeConverter
    fun toEpisodeStatusEnum(value: Int?): EpisodeStatusEnum? {
        return if (value == null) null else EpisodeStatusEnum.values()[value]
    }

    @TypeConverter
    fun toInt(value: EpisodeStatusEnum?): Int? {
        return value?.ordinal
    }

    @TypeConverter
    fun listToInt(value: List<EpisodeStatusEnum>): List<Int> {
        return value.mapNotNull { toInt(it) }
    }

    @TypeConverter
    fun listToEpisodeStatus(value: List<Int>): List<EpisodeStatusEnum> {
        return value.mapNotNull { toEpisodeStatusEnum(it) }
    }
}
