package au.com.shiftyjelly.pocketcasts.models.converter

import androidx.room.TypeConverter
import java.time.Instant

class InstantConverter {
    @TypeConverter
    fun fromDbValue(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)

    @TypeConverter
    fun toDbValue(value: Instant?): Long? = value?.toEpochMilli()
}
