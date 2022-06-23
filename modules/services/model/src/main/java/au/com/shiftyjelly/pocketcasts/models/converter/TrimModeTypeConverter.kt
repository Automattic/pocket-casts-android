package au.com.shiftyjelly.pocketcasts.models.converter

import androidx.room.TypeConverter
import au.com.shiftyjelly.pocketcasts.models.type.TrimMode

class TrimModeTypeConverter {
    @TypeConverter
    fun toTrimMode(value: Int?): TrimMode {
        return TrimMode.values().getOrNull(value ?: 0) ?: TrimMode.OFF
    }

    @TypeConverter
    fun toString(value: TrimMode?): Int {
        return value?.ordinal ?: 0
    }
}
