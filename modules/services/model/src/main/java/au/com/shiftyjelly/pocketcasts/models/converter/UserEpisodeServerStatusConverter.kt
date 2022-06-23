package au.com.shiftyjelly.pocketcasts.models.converter

import androidx.room.TypeConverter
import au.com.shiftyjelly.pocketcasts.models.type.UserEpisodeServerStatus

class UserEpisodeServerStatusConverter {
    @TypeConverter
    fun toUserEpisodeServerStatus(value: Int?): UserEpisodeServerStatus? {
        return if (value == null) null else UserEpisodeServerStatus.values()[value]
    }

    @TypeConverter
    fun toInt(value: UserEpisodeServerStatus?): Int? {
        return value?.ordinal
    }
}
