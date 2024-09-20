package au.com.shiftyjelly.pocketcasts.models.converter

import androidx.room.TypeConverter
import au.com.shiftyjelly.pocketcasts.models.to.AutoArchiveLimit

class AutoArchiveLimitTypeConverter {
    @TypeConverter
    fun fromInt(value: Int): AutoArchiveLimit {
        return AutoArchiveLimit.fromServerId(value) ?: AutoArchiveLimit.None
    }

    @TypeConverter
    fun toInt(value: AutoArchiveLimit): Int {
        return value.serverId
    }
}
