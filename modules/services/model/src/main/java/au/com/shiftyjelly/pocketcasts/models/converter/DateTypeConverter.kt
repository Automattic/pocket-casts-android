package au.com.shiftyjelly.pocketcasts.models.converter

import androidx.room.TypeConverter
import java.util.Date

class DateTypeConverter {

    @TypeConverter
    fun toDate(value: Long?): Date? {
        return if (value == null) null else Date(value)
    }

    @TypeConverter
    fun toLong(value: Date?): Long? {
        return value?.time
    }
}
