package au.com.shiftyjelly.pocketcasts.models.converter

import androidx.room.TypeConverter
import au.com.shiftyjelly.pocketcasts.models.to.AutoArchiveInactive

class AutoArchiveInactiveTypeConverter {
    @TypeConverter
    fun fromInt(value: Int?): AutoArchiveInactive {
        return value?.let { AutoArchiveInactive.fromIndex(it) } ?: AutoArchiveInactive.Default
    }

    @TypeConverter
    fun toInt(value: AutoArchiveInactive?): Int {
        return value?.index ?: AutoArchiveInactive.Default.index
    }
}
